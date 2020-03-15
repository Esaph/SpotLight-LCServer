/*
 *  Copyright (C) Esaph, Julian Auguscik - All Rights Reserved
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Julian Auguscik <esaph.re@gmail.com>, March  2020
 *
 */

package Esaph;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.sql.SQLException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;

import LifeCloudServerMain.LifeCloudServer;

public class SendInformationToUser extends Thread
{
	private static final String KeystoreFilePathClient = "/usr/server/serverMSG.jks";
	private static final String TrustStoreFilePathClient = "/usr/server/clienttruststoreFORSERVER.jks";
	private static final String KeystorePassClient = "50b605f02e";
	private static final String TruststorePasswordClient = "28612@1587";
	
	private LifeCloudServer lifecloudServer;
	private LogUtilsEsaph logUtilsRequest;
	private JSONObject message;
	private Connection connection;
	private static final String queryInsertNewMesage = "INSERT INTO Messages (UID_RECEIVER, MESSAGE) values (?, ?)";
	
	public SendInformationToUser(JSONObject message, LogUtilsEsaph logUtilsRequest, LifeCloudServer lifecloudServer)
	{
		this.lifecloudServer = lifecloudServer;
		this.logUtilsRequest = logUtilsRequest;
		this.message = message;
	}
	
	private Connection getConnectionToSql() throws InterruptedException, SQLException
	{
		return (Connection) lifecloudServer.getPLServerPool().getConnectionFromPool();
	}
	
	@Override
	public void run()
	{ //Hier werden keine überprüfungen benötigt, nur lediglich ob die freunde sind.
		boolean shouldSaveIt = true;

		PreparedStatement prStoreMessage = null;

		try
		{
			if(!this.message.has("TIME"))
			{
				this.message.put("TIME", System.currentTimeMillis());
			}
			
			this.connection = this.getConnectionToSql();
			SSLContext sslContext;
			KeyStore trustStore = KeyStore.getInstance("JKS");
			trustStore.load(new FileInputStream(SendInformationToUser.TrustStoreFilePathClient), SendInformationToUser.TruststorePasswordClient.toCharArray());
			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(SendInformationToUser.KeystoreFilePathClient), SendInformationToUser.KeystorePassClient.toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keystore, SendInformationToUser.KeystorePassClient.toCharArray());

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(trustStore);

			sslContext = SSLContext.getInstance("TLS"); 
			TrustManager[] trustManagers = tmf.getTrustManagers(); 
			sslContext.init(kmf.getKeyManagers(), trustManagers, null);
			
		    SSLSocketFactory sslClientSocketFactory = sslContext.getSocketFactory();
			
			SSLSocket socket = (SSLSocket) sslClientSocketFactory.createSocket("127.0.0.1", 1030);
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            socket.setSoTimeout(10000);
            writer.println(this.message.toString());
            writer.flush();
            shouldSaveIt = false;
			
            String result = reader.readLine();
            
            if(!result.equals("1") || shouldSaveIt)
            {
            	JSONArray RECEIVERS = new JSONArray(this.message.getString("EMPF"));
				this.message.remove("EMPF");
				
				for(int counter = 0; counter < RECEIVERS.length(); counter++)
				{
					prStoreMessage = (PreparedStatement) connection.prepareStatement(SendInformationToUser.queryInsertNewMesage);
					prStoreMessage.setLong(1, RECEIVERS.getLong(counter));
					prStoreMessage.setString(2, this.message.toString());
					prStoreMessage.executeUpdate();
					prStoreMessage.close();
				}
            }
            socket.close();
            writer.close();
            reader.close();
            this.logUtilsRequest.writeLog("Message sent");
		}
		catch(Exception ec)
		{
			this.logUtilsRequest.writeLog("SendInformationToUser failed: " + ec);
		}
		finally
		{
			try
			{
				if(prStoreMessage != null)
				{
					prStoreMessage.close();
				}

				if(prStoreMessage != null)
				{
					prStoreMessage.close();
				}
			}
			catch (Exception ec)
			{

			}

			if(shouldSaveIt)
			{
				PreparedStatement prStoreMessageWhenFailed = null;
				try
				{
					JSONArray RECEIVERS = new JSONArray(this.message.getString("EMPF"));
					this.message.remove("EMPF");
					
					for(int counter = 0; counter < RECEIVERS.length(); counter++)
					{
						prStoreMessageWhenFailed = (PreparedStatement) connection.prepareStatement(SendInformationToUser.queryInsertNewMesage);
						prStoreMessageWhenFailed.setLong(1, RECEIVERS.getLong(counter));
						prStoreMessageWhenFailed.setString(2, this.message.toString());
						prStoreMessageWhenFailed.executeUpdate();
						prStoreMessageWhenFailed.close();
					}
				}
				catch(Exception ecFATAL)
				{
					this.logUtilsRequest.writeLog("SendInformationToUser failed to store failed msg to database (shouldSaveIt) was true: " + ecFATAL);
				}
				finally
				{
					try
					{
						if(prStoreMessageWhenFailed != null)
						{
							prStoreMessageWhenFailed.close();
						}
					}
					catch (Exception ec)
					{
					}
				}
			}
			
			this.connection = this.lifecloudServer.getPLServerPool().returnConnectionToPool(this.connection);
		}
	}
}




