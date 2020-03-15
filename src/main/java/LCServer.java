/*
 *  Copyright (C) Esaph, Julian Auguscik - All Rights Reserved
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Julian Auguscik <esaph.re@gmail.com>, March  2020
 *
 */

import java.io.IOException;

import LifeCloudServerMain.LifeCloudServer;

public class LCServer
{
	public static void main(String[] args) throws IOException
	{
		LifeCloudServer lifecloudServer = new LifeCloudServer();
		lifecloudServer.startServer();
	}
}
