/*
 *  Copyright (C) Esaph, Julian Auguscik - All Rights Reserved
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Julian Auguscik <esaph.re@gmail.com>, March  2020
 *
 */

package Commands;

import Esaph.EsaphDataPrefix;
import Esaph.EsaphImageScaler;
import Esaph.EsaphStoragePaths;
import Esaph.LogUtilsEsaph;
import LifeCloudServerMain.LifeCloudServer;
import com.mysql.jdbc.PreparedStatement;
import org.json.JSONObject;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;

public class DownloadLifeCloudDataOnlyOwn extends EsaphCommandLC //Only own is for secutirty, that no one can download it when not thread uid is matching. Like a password protection.
{
    public DownloadLifeCloudDataOnlyOwn(LifeCloudServer lifeCloudServer, LifeCloudServer.RequestHandler requestHandler, LogUtilsEsaph logUtilsRequest) {
        super(lifeCloudServer, requestHandler, logUtilsRequest);
    }

    @Override
    public void run() throws Exception //Is for image and video, the same command. But for video we should transmitt scaling values.
    {
        File sonderAnfertigung = null;

        try
        {
            JSONObject jsonMessage = super.requestHandler().getJSONMessage();
            File filePic = getStoringFile(jsonMessage.getString("PID"));
            if(filePic.exists())
            {
                if(jsonMessage.has("VW") && jsonMessage.has("VH"))
                {
                    int viewsWidth = jsonMessage.getInt("VW");
                    int viewsHeight = jsonMessage.getInt("VH");
                    sonderAnfertigung = EsaphImageScaler.esaphScaleImageForClient(filePic,
                            this.getTempFile(EsaphDataPrefix.JPG_PREFIX),
                            new Dimension(viewsWidth, viewsHeight));

                    if(sonderAnfertigung.exists())
                    {
                        this.sendFile(sonderAnfertigung);
                        sonderAnfertigung.delete();
                    }
                }
                else
                {
                    this.sendFile(filePic);
                }
            }
        }
        catch (Exception ec)
        {
        }
        finally
        {
            if(sonderAnfertigung != null && sonderAnfertigung.exists())
            {
                sonderAnfertigung.delete();
            }
        }
    }


    private File getStoringFile(String PID)
    {
        StringBuilder builderCache = new StringBuilder();
        builderCache.append(EsaphStoragePaths.LIFECLOUD_FILES_PATH);
        builderCache.append(File.separator);
        builderCache.append(this.getFolderFilePath(PID));
        return new File(builderCache.toString());
    }

    private String getFolderFilePath(String mainDirectory) //Überprüft auch automatisch ob das datei Limit erreicht wurde, wenn ja dann wird ein neuer ordner angelegt.
    {
        StringBuilder stringBuilder = new StringBuilder();
        for(int counter = 0; counter < mainDirectory.length(); counter++)
        {
            stringBuilder.append(mainDirectory.substring(counter, counter+1));
            stringBuilder.append(File.separator);
        }
        stringBuilder.append(mainDirectory);
        return stringBuilder.toString();
    }



    private void sendFile(File file) throws IOException
    {
        DataOutputStream dos = null;
        FileInputStream fis = null;

        try
        {
            super.logUtilsRequest().writeLog("SENDING Image: " + file.getAbsolutePath());
            dos = new DataOutputStream(super.requestHandler().getSocket().getOutputStream());
            fis = new FileInputStream(file);

            long length = file.length();
            super.requestHandler().getWriter().println(length);
            byte[] buffer = new byte[4096];
            while (fis.read(buffer) > 0)
            {
                dos.write(buffer);
                dos.flush();
            }
            super.logUtilsRequest().writeLog("Post I sent.");
        }
        catch (Exception ec)
        {
            super.logUtilsRequest().writeLog("Sent file failed lifecloud: " + ec);
        }
        finally
        {
            if(fis != null)
            {
                fis.close();
            }

            if(dos != null)
            {
                dos.close();
            }
        }
    }

    private File getTempFile(String prefix) //Präfix = jpg oder mp4, datei format.
    {
        StringBuilder builderCache = new StringBuilder();
        builderCache.append(EsaphStoragePaths.dirTEMP);
        builderCache.append(super.requestHandler().getThreadUsername());
        builderCache.append("-");
        builderCache.append(System.currentTimeMillis());
        builderCache.append(".");
        builderCache.append(prefix);
        return new File(builderCache.toString());
    }
}
