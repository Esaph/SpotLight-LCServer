/*
 *  Copyright (C) Esaph, Julian Auguscik - All Rights Reserved
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Julian Auguscik <esaph.re@gmail.com>, March  2020
 *
 */

package Commands;

import Esaph.*;
import LifeCloudServerMain.LifeCloudServer;
import com.mysql.jdbc.PreparedStatement;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

public class UploadLifeCloudPostVideo extends EsaphCommandLC
{
    private static final String queryInsertNewHashtags = "INSERT INTO TAGS (UID_POST_FROM, UID_TAGER, PID, TAG_NAME) values (?, ?, ?, ?)";
    private static final String QUERY_INSERT_NEW_LIFECLOUD_POST = "INSERT INTO LifeCloud (UID, PID, DESCRIPTION, DATA_TYPE, POST_TYPE) values (?, ?, ?, ?, ?)";

    public UploadLifeCloudPostVideo(LifeCloudServer lifeCloudServer, LifeCloudServer.RequestHandler requestHandler, LogUtilsEsaph logUtilsRequest)
    {
        super(lifeCloudServer, requestHandler, logUtilsRequest);
    }

    @Override
    public void run() throws Exception
    {
        PreparedStatement prInsertNewLifeCloudUpload = null;

        try
        {
            String description = super.requestHandler().getJSONMessage().optString("DES", "");

            String PID = this.generatePID(super.requestHandler().getThreadUsername());
            File fileLifeCloudUpload = this.getStoringFile(PID);
            File fileTemp = this.getTempFile(EsaphDataPrefix.JPG_PREFIX);
            JSONArray jsonArrayHashtags = super.requestHandler().getJSONMessage().getJSONArray("ARR_EHT");

            if(!isHashtagArrayValid(jsonArrayHashtags) || description.length() > EsaphMaxSizes.SIZE_MAX_BESCHREIBUNG_LENGTH)
                return;

            super.requestHandler().returnConnectionToPool();
            if(this.uploadVideo(fileTemp, fileLifeCloudUpload))
            {
                super.requestHandler().getConnectionToSql();
                prInsertNewLifeCloudUpload =
                        (PreparedStatement) super.requestHandler().getCurrentConnectionToSql().prepareStatement(UploadLifeCloudPostVideo.QUERY_INSERT_NEW_LIFECLOUD_POST);
                prInsertNewLifeCloudUpload.setLong(1, super.requestHandler().getThreadUID());
                prInsertNewLifeCloudUpload.setString(2, PID);
                prInsertNewLifeCloudUpload.setString(3, description);
                prInsertNewLifeCloudUpload.setShort(4, CMTypes.FVID);
                prInsertNewLifeCloudUpload.setShort(5, EsaphLifeCloudTypeHelper.LIFECLOUD_TYPE_LIFECAPTURECAM);
                int result = prInsertNewLifeCloudUpload.executeUpdate();
                prInsertNewLifeCloudUpload.close();

                for(int counterHashtags = 0; counterHashtags < jsonArrayHashtags.length(); counterHashtags++)
                {
                    JSONObject jsonObjectHashtag = jsonArrayHashtags.getJSONObject(counterHashtags);
                    PreparedStatement preparedAddHashtagsForPost = (PreparedStatement) super.requestHandler().getCurrentConnectionToSql()
                            .prepareStatement(UploadLifeCloudPostVideo.queryInsertNewHashtags);
                    preparedAddHashtagsForPost.setLong(1, super.requestHandler().getThreadUID());
                    preparedAddHashtagsForPost.setLong(2, super.requestHandler().getThreadUID());
                    preparedAddHashtagsForPost.setString(3, PID);
                    preparedAddHashtagsForPost.setString(4, jsonObjectHashtag.getString("TAG"));
                    preparedAddHashtagsForPost.executeUpdate();
                    preparedAddHashtagsForPost.close();
                }

                if(result > 0)
                {
                    super.requestHandler().getWriter().println(PID); //DONE
                }
            }
        }
        catch (Exception ec)
        {

        }
        finally
        {
            if(prInsertNewLifeCloudUpload != null)
            {
                prInsertNewLifeCloudUpload.close();
            }
        }
    }

    private String generatePID(String username)
    {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32) + username;
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


    private boolean uploadVideo(File TEMP, File FILE_VIDEO) throws Exception
    {
        try
        {
            super.requestHandler().getWriter().println("1");
            super.logUtilsRequest().writeLog("LifeCloud handling video, reading length...");
            long maxLength = Long.parseLong(super.requestHandler().readDataCarefully(10));
            super.logUtilsRequest().writeLog("LifeCloud handling video, length: " + maxLength);
            super.requestHandler().getWriter().println("1");
            long readed = 0;

            if(maxLength > EsaphMaxSizes.SIZE_MAX_VIDEO)
            {
                return false;
            }

            DataInputStream dateiInputStream = null;
            FileOutputStream dateiStream = null;
            super.logUtilsRequest().writeLog("LifeCloud post video wird hochgeladen.");
            dateiInputStream = new DataInputStream(super.requestHandler().getSocket().getInputStream());
            dateiStream = new FileOutputStream(TEMP);

            int count;
            byte[] buffer = new byte[(int) maxLength]; // or 4096, or more
            while ((count = dateiInputStream.read(buffer)) > 0) //Ließt image
            {
                dateiStream.write(buffer, 0, count);
                readed = readed+count;
                if(maxLength <= readed)
                {
                    break;
                }
            }
            super.logUtilsRequest().writeLog("Finished");
            dateiStream.close();

            if(this.isVideoFile(TEMP)) //Überprüft datei. this.isImageFile(FILE_AUDIO)
            {
                super.logUtilsRequest().writeLog("LifeCloud post video WRITTEN.");
                FileUtils.copyFile(TEMP, FILE_VIDEO);
                return true;
            }
            else
            {
                if(FILE_VIDEO != null) //nur ein mal löschen. :) =) :D
                {
                    FILE_VIDEO.delete();
                }
            }
        }
        catch (Exception ec)
        {

        }
        finally
        {
            if(TEMP != null) //nur ein mal löschen. :) =) :D
            {
                TEMP.delete();
            }
        }

        return false;
    }


    private boolean isVideoFile(File file)
    {
        super.logUtilsRequest().writeLog("WARNING: MIMETYPE ISNT CHECKED");
        return true;
    }


    private boolean isHashtagArrayValid(JSONArray jsonArrayHashtags) throws JSONException
    {
        if(jsonArrayHashtags == null)
            return false;

        for(int counter = 0; counter < jsonArrayHashtags.length(); counter++)
        {
            JSONObject json = jsonArrayHashtags.getJSONObject(counter);
            if(!json.has("TAG")
                    || json.getString("TAG").isEmpty()
                    || json.getString("TAG").length() > EsaphMaxSizes.SIZE_MAX_HASHTAG_LENGTH)
            {
                return false;
            }
        }

        return true;
    }
}
