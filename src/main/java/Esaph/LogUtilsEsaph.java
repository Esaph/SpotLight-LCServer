/*
 *  Copyright (C) Esaph, Julian Auguscik - All Rights Reserved
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Julian Auguscik <esaph.re@gmail.com>, March  2020
 *
 */

package Esaph;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class LogUtilsEsaph
{
	private String InetAddress;
	private String ServerType;
	private String Username;
	private FileWriter printer;
	private SimpleDateFormat simple;
	
	
	public LogUtilsEsaph(final File rootPath, final String fileName, final String ServerType, final String InetAddress, final String Username) throws IOException
	{
		this.Username = Username;
		this.InetAddress = InetAddress;
		this.ServerType = ServerType;
		this.simple = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss");
		
		File fileCurrent = new File(rootPath.getAbsolutePath(), fileName);
		this.printer = new FileWriter(fileCurrent, true);
	}
	
	
	public void closeFile()
	{
		try
		{
			this.printer.close();
		}
		catch(Exception ec)
		{
			try
			{
				File file = new File("/usr/server/bigProblem.log");
				file.createNewFile();
				FileWriter writer = new FileWriter(file);
				writer.append(this.ServerType + "-" + this.simple.format(System.currentTimeMillis()) + "@" + this.InetAddress + ": " + ec + System.lineSeparator());
				writer.close();
			}
			catch(Exception ec2)
			{
				System.out.println(this.ServerType + "-" + this.simple.format(System.currentTimeMillis()) + "@" + this.InetAddress + ": Ja arschlecken 2.0 konnte datei nicht schlieﬂen:  " + ec2 + System.lineSeparator());
			}
		}
	}
	
	
	public synchronized void writeLog(String log)
	{
		try
		{
			StringBuilder builder = new StringBuilder();
			builder.append(this.ServerType);
			builder.append("-");
			builder.append(this.simple.format(System.currentTimeMillis()));
			builder.append("\t@\t");
			builder.append(this.InetAddress);
			builder.append(", ");
			builder.append(this.Username);
			builder.append(": ");
			builder.append(log);
			builder.append(System.lineSeparator());
			
			this.printer.append(builder.toString());
			this.printer.flush();
		}
		catch(Exception ec)
		{
			try
			{
				File file = new File("/usr/server/bigProblem.log");
				file.createNewFile();
				FileWriter writer = new FileWriter(file);
				writer.append(this.ServerType + "-" + this.simple.format(System.currentTimeMillis()) + "@" + this.InetAddress + ": " + ec + System.lineSeparator());
				writer.close();
			}
			catch(Exception ec2)
			{
				System.out.println(this.ServerType + "-" + this.simple.format(System.currentTimeMillis()) + "@" + this.InetAddress + ": Ja arschlecken: " + ec2 + System.lineSeparator());
			}
		}
	}
	
	public void setUsername(String Username)
	{
		this.Username = Username;
	}
}
