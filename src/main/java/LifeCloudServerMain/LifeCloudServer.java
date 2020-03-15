/*
 *  Copyright (C) Esaph, Julian Auguscik - All Rights Reserved
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Julian Auguscik <esaph.re@gmail.com>, March  2020
 *
 */

package LifeCloudServerMain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import Commands.*;
import org.json.JSONObject;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;

import Esaph.LogUtilsEsaph;

public class LifeCloudServer extends Thread
{
	private LogUtilsEsaph logUtilsMain;
	private static final String mainServerLogPath = "/usr/server/Log/LCServer/";
	private static final String ServerType = "LifeCloudServer";
	private static final String placeholder = "LifeCloudServer: ";
	private SSLServerSocket serverSocket;
	private static final int port = 1032;
	private final HashMap<String, Integer> connectionMap = new HashMap<String, Integer>();
	private SQLPool pool;
	
	//FCM INFORMATION HANDLER;
	
	private static final ThreadPoolExecutor executorSubThreads = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
            100,
            15,
            TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());
	
	private static final ThreadPoolExecutor executorMainThread = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
            100,
            15,
            TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());
	
	
	public ThreadPoolExecutor getExecutorSubThreads()
	{
		return executorSubThreads;
	}
	
	
	public SQLPool getPLServerPool()
	{
		return this.pool;
	}
	
	
	public LifeCloudServer() throws IOException
	{
		logUtilsMain = new LogUtilsEsaph(new File(LifeCloudServer.mainServerLogPath), "LifeCloudServer.log", LifeCloudServer.ServerType, "127.0.0.1", "ES_ROOT");
		Timer timer = new Timer();
		timer.schedule(new unfreezeConnections(), 0, 60000);
		try
		{
			pool = new SQLPool();
			this.logUtilsMain.writeLog(LifeCloudServer.placeholder + "Thread pool loaded().");
		}
		catch(Exception ec)
		{
			this.logUtilsMain.writeLog(LifeCloudServer.placeholder + "Thread pool failed to load: " + ec);
		}
	}
	
	
	public void startServer()
	{
		try
		{
			this.initSSLKey();
		    SSLServerSocketFactory sslServerSocketFactory = this.sslContext.getServerSocketFactory();
		    this.serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(LifeCloudServer.port);
			this.start();
			this.logUtilsMain.writeLog("server started.");
		}
		catch(Exception io)
		{
			this.logUtilsMain.writeLog("Exception(Starting server): " + io);
			System.exit(0);
		}
	}
	
	
	private static final String KeystoreFilePath = "/usr/server/ECCMasterKey.jks";
	private static final String TrustStoreFilePath = "/usr/server/servertruststore.jks";
	


	
	private static final String KeystorePass = "8db3626e47";
	private static final String TruststorePassword = "842407c248";
	private SSLContext sslContext;
	
	private void initSSLKey() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, UnrecoverableKeyException, KeyManagementException
	{
		this.logUtilsMain.writeLog(LifeCloudServer.placeholder + "Setting up SSL-Encryption");
		KeyStore trustStore = KeyStore.getInstance("JKS");
		trustStore.load(new FileInputStream(LifeCloudServer.TrustStoreFilePath), LifeCloudServer.TruststorePassword.toCharArray());
		this.logUtilsMain.writeLog(LifeCloudServer.placeholder + "SSL-Encryption TrustStore VALID.");
		KeyStore keystore = KeyStore.getInstance("JKS");
		keystore.load(new FileInputStream(LifeCloudServer.KeystoreFilePath), LifeCloudServer.KeystorePass.toCharArray());
		this.logUtilsMain.writeLog(LifeCloudServer.placeholder + "SSL-Encryption Keystore VALID.");
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keystore, LifeCloudServer.KeystorePass.toCharArray());

		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509"); 
		tmf.init(trustStore);

		sslContext = SSLContext.getInstance("TLS"); 
		TrustManager[] trustManagers = tmf.getTrustManagers(); 
		sslContext.init(kmf.getKeyManagers(), trustManagers, null);
		this.logUtilsMain.writeLog(LifeCloudServer.placeholder + "SSL-Encryption OK.");
	}
	
	private class unfreezeConnections extends TimerTask
	{
	    public void run()
	    {
	    	synchronized(connectionMap)
	    	{
	    		if(connectionMap.size() != 0)
	    		{
	    			logUtilsMain.writeLog(LifeCloudServer.placeholder + "Clearing IP-HASHMAP");
	    			connectionMap.clear();
	    		}
	    	}	    	
	    }
	}
	
	
	
	private static final int MAX_CONN_PER_MINUTE = 250; //Problem mit liste in den bilder... muss halt so eine hohe zahl sein. Falls ein nutzer nach unten scrollt.
	
	@Override
	public void run()
	{
		while(true)
		{
			try
			{
				SSLSocket socket = (SSLSocket) serverSocket.accept();
				if(this.connectionMap.get(socket.getInetAddress().toString()) != null)
				{
					if(this.connectionMap.get(socket.getInetAddress().toString()) >= LifeCloudServer.MAX_CONN_PER_MINUTE)
					{
						socket.close();
					}
					else
					{
						this.connectionMap.put(socket.getInetAddress().toString(),  this.connectionMap.get(socket.getInetAddress().toString()) + 1);
						this.logUtilsMain.writeLog("Connection: " + socket.getInetAddress());
						
						LifeCloudServer.executorMainThread.submit(new RequestHandler(socket));
					}
				}
				else
				{
					this.connectionMap.put(socket.getInetAddress().toString(), 1);
					this.logUtilsMain.writeLog("Connection: " + socket.getInetAddress());
					LifeCloudServer.executorMainThread.submit(new RequestHandler(socket));
				}
			}
			catch(Exception ec)
			{
				this.logUtilsMain.writeLog("InforamtionServer(ACCEPT_ERROR): " + ec);
			}
		}
	}

	private static final String CMD_UploadLifeCloudPostImage = "LCULCI";
	private static final String CMD_UploadLifeCloudPostVideo = "LCULCV";
	private static final String CMD_GET_POST_DATA = "LCGPD";
	private static final String CMD_DELETE_POST = "LCDLP";
	private static final String CMD_GetAllLifeCloudPostsLimited = "LFGAP";

	private static final String queryLookUpUID = "SELECT UID FROM Users WHERE Benutzername=?";
	private static final String QUERY_CHECK_SESSION = "SELECT SID FROM Sessions WHERE SID=? AND UID=?";
	
	public class RequestHandler extends Thread
	{
		private LogUtilsEsaph logUtilsRequest;
		private JSONObject jsonMessage;
		private SSLSocket socket;
		private PrintWriter writer;
		private BufferedReader reader;
		private Connection connection;
		private long ThreadUID;
		private String ThreadUsername;
		
		private RequestHandler(SSLSocket socket)
		{
			this.socket = socket;
		}
		
		public PrintWriter getWriter()
		{
			return this.writer;
		}
		
		public SSLSocket getSocket()
		{
			return this.socket;
		}
		
		public void returnConnectionToPool()
		{
			this.connection = pool.returnConnectionToPool(this.connection);
		}
		
		public void getConnectionToSql() throws InterruptedException, SQLException
		{
			//Is blocking until we have got a connection. Or a exception occurs!
			this.connection = (Connection) pool.getConnectionFromPool();
		}

		public Connection getCurrentConnectionToSql()
		{
			return this.connection;
		}

		public String readDataCarefully(int bufferSize) throws Exception
		{
			String msg = this.reader.readLine();
			if(msg == null || msg.length() > bufferSize)
			{
				throw new Exception("Exception: msg " + msg + " length: " + msg.length() + ">" + bufferSize);
			}
			this.logUtilsRequest.writeLog("MSG: " + msg);
			return msg;
		}

		public String getThreadUsername()
		{
			return this.ThreadUsername;
		}


		public long getThreadUID()
		{
			return this.ThreadUID;
		}


		public JSONObject getJSONMessage()
		{
			return this.jsonMessage;
		}
		
		@Override
		public void run()
		{
			try
			{
				this.logUtilsRequest = new LogUtilsEsaph(new File(LifeCloudServer.mainServerLogPath),
						socket.getInetAddress() + ".log", 
						LifeCloudServer.ServerType,
						socket.getInetAddress().toString(), "");
				
				this.socket.setSoTimeout(15000);
				this.writer = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8), true);
				this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8));
				this.jsonMessage = new JSONObject(this.readDataCarefully(5000));
				this.getConnectionToSql();

				if(checkSID())
				{
					this.logUtilsRequest.setUsername(this.ThreadUsername);
					this.writer.println("1"); //WEITER
					String anfrage = this.jsonMessage.getString("LCS");
					this.logUtilsRequest.writeLog("ANFRAGE: " + anfrage);
					
					if(anfrage.equals(LifeCloudServer.CMD_UploadLifeCloudPostImage))
					{
						new UploadLifeCloudPostImage(LifeCloudServer.this, this, this.logUtilsRequest).run();
					}
					else if(anfrage.equals(LifeCloudServer.CMD_UploadLifeCloudPostVideo))
					{
						new UploadLifeCloudPostVideo(LifeCloudServer.this, this, this.logUtilsRequest).run();
					}
					else if(anfrage.equals(LifeCloudServer.CMD_GET_POST_DATA)) //Sending image or video to user.
					{
						new DownloadLifeCloudDataOnlyOwn(LifeCloudServer.this, this, this.logUtilsRequest).run();
					}
					else if(anfrage.equals(LifeCloudServer.CMD_DELETE_POST))
					{
						new DeleteLifeCloudPost(LifeCloudServer.this, this, this.logUtilsRequest).run();
					}
					else if(anfrage.equals(LifeCloudServer.CMD_GetAllLifeCloudPostsLimited))
					{
						new LoadMoreLifeCloudPostsLimited(LifeCloudServer.this, this, this.logUtilsRequest).run();
					}
				}
			}
			catch(Exception ec)
			{
				this.logUtilsRequest.writeLog("LifeCloudServer_requestHandler failed: " + ec);
			}
			finally
			{
				this.connection = pool.returnConnectionToPool(this.connection);
			}
		}

		public boolean checkSession(long UID, String SID)
		{
			PreparedStatement qSID = null;
			ResultSet result = null;

			try
			{
				this.logUtilsRequest.writeLog("Checking session");
				qSID = (PreparedStatement) this.connection.prepareStatement(LifeCloudServer.QUERY_CHECK_SESSION);
				qSID.setString(1, SID);
				qSID.setLong(2, UID);
				result = qSID.executeQuery();
				if(result.next())
				{
					this.logUtilsRequest.writeLog("Session: NEXT()");
					return true;
				}
				else
				{
					this.logUtilsRequest.writeLog("Session: !=NEXT()");
					return false;
				}
			}
			catch(Exception ec)
			{
				this.logUtilsRequest.writeLog("Exception: " + ec);
				return false;
			}
			finally
			{
				try
				{
					if(qSID != null)
					{
						qSID.close();
					}

					if(result != null)
					{
						result.close();
					}
				}
				catch (Exception ec)
				{

				}
			}
		}

		public long lookUpUID(String username)
		{
			PreparedStatement prLookUp = null;
			ResultSet lookUpResult = null;
			try
			{
				this.logUtilsRequest.writeLog("USERNAME:" + username);
				prLookUp = (PreparedStatement) this.connection
						.prepareStatement(LifeCloudServer.queryLookUpUID);
				prLookUp.setString(1, username);

				lookUpResult = prLookUp.executeQuery();

				long UID = -1;

				if (lookUpResult.next())
				{
					this.logUtilsRequest.writeLog("UID OK.");
					UID = lookUpResult.getLong(1);
				}
				else {
					this.logUtilsRequest.writeLog("UID WRONG.");
				}

				return UID;

			}
			catch (Exception ec)
			{
				this.logUtilsRequest.writeLog("(lookUpUID): FATAL ERROR: " + ec);
				return -1;
			}
			finally
			{
				try
				{
					if(prLookUp != null)
					{
						prLookUp.close();
					}

					if(lookUpResult != null)
					{
						lookUpResult.close();
					}
				}
				catch (Exception ec)
				{
				}
			}
		}

		private boolean checkSID()
		{
			try
			{
				long UID = lookUpUID(this.jsonMessage.getString("USRN"));
				String SID = this.jsonMessage.getString("SID");
				if(UID > 0)
				{
					if(checkSession(UID, SID))
					{
						this.logUtilsRequest.writeLog("Session OK.");
						this.ThreadUsername = this.jsonMessage.getString("USRN");
						this.ThreadUID = UID;
						return true;
					}
					else
					{
						this.logUtilsRequest.writeLog("Session WRONG.");
						return false;
					}
				}
				else
				{
					this.logUtilsRequest.writeLog(" client has passed a null object!");
					return false;
				}
			}
			catch(Exception ec)
			{
				this.logUtilsRequest.writeLog("(checkSID): FATAL ERROR");
				return false;
			}
		}
	}
	
	
}
