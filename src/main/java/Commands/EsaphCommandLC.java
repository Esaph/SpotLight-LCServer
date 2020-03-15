/*
 *  Copyright (C) Esaph, Julian Auguscik - All Rights Reserved
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Julian Auguscik <esaph.re@gmail.com>, March  2020
 *
 */

package Commands;

import Esaph.LogUtilsEsaph;
import LifeCloudServerMain.LifeCloudServer;

public abstract class EsaphCommandLC
{
	private LogUtilsEsaph logUtilsRequest;
	private LifeCloudServer.RequestHandler requestHandler;
	private LifeCloudServer lifeCloudServer;
	
	public EsaphCommandLC(LifeCloudServer lifeCloudServer, LifeCloudServer.RequestHandler requestHandler, LogUtilsEsaph logUtilsRequest)
	{
		this.lifeCloudServer = lifeCloudServer;
		this.requestHandler = requestHandler;
		this.logUtilsRequest = logUtilsRequest;
	}

	public LifeCloudServer.RequestHandler requestHandler() {
		return requestHandler;
	}

	public LifeCloudServer lifeCloudServer()
	{
		return this.lifeCloudServer;
	}
	
	public LogUtilsEsaph logUtilsRequest()
	{
		return this.logUtilsRequest;
	} 
	
	public abstract void run() throws Exception;
	
}
