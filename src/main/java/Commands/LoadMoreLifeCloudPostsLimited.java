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
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoadMoreLifeCloudPostsLimited extends EsaphCommandLC
{
    private static final String QUERY_GetOwnLifeCloudPosts = "SELECT * FROM LifeCloud  WHERE UID=? ORDER BY TIME DESC LIMIT ?, 30";
    private static final String queryGetHashtagsFromPost = "SELECT TAG_NAME FROM TAGS WHERE PID=?";


    public LoadMoreLifeCloudPostsLimited(LifeCloudServer lifeCloudServer, LifeCloudServer.RequestHandler requestHandler, LogUtilsEsaph logUtilsRequest) {
        super(lifeCloudServer, requestHandler, logUtilsRequest);
    }

    @Override
    public void run() throws Exception
    {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        PreparedStatement prGetHashtagFromPost = null;
        ResultSet resultHashtags = null;

        try
        {
            preparedStatement = (PreparedStatement) super.requestHandler().getCurrentConnectionToSql()
                    .prepareStatement(LoadMoreLifeCloudPostsLimited.QUERY_GetOwnLifeCloudPosts);
            preparedStatement.setLong(1, super.requestHandler().getThreadUID());
            preparedStatement.setInt(2, super.requestHandler().getJSONMessage().getInt("SF"));

            resultSet = preparedStatement.executeQuery();

            JSONArray jsonArray = new JSONArray();

            while(resultSet.next())
            {
                JSONObject jsonObject = new JSONObject();

                prGetHashtagFromPost = (PreparedStatement)
                        super.requestHandler()
                                .getCurrentConnectionToSql()
                                .prepareStatement(LoadMoreLifeCloudPostsLimited.queryGetHashtagsFromPost);

                prGetHashtagFromPost.setString(1, resultSet.getString("PID"));
                resultHashtags = prGetHashtagFromPost.executeQuery();
                JSONArray jsonArrayHashtags = new JSONArray();
                while(resultHashtags.next())
                {
                    JSONObject json = new JSONObject();
                    json.put("TAG", resultHashtags.getString("TAG_NAME"));
                    jsonArrayHashtags.put(json);
                }

                jsonObject.put("ARR_EHT", jsonArrayHashtags);

                jsonObject.put("DESC", resultSet.getString("DESCRIPTION"));
                jsonObject.put("PID", resultSet.getString("PID"));
                jsonObject.put("TP", resultSet.getTimestamp("TIME").getTime());
                jsonObject.put("DT", resultSet.getString("DATA_TYPE"));
                jsonObject.put("PT", resultSet.getString("POST_TYPE"));
                jsonArray.put(jsonObject);
            }

            super.requestHandler().getWriter().println(jsonArray.toString());
        }
        catch (Exception ec)
        {

        }
        finally
        {
            if(preparedStatement != null)
            {
                preparedStatement.close();
            }

            if(resultSet != null)
            {
                resultSet.close();
            }

            if(prGetHashtagFromPost != null)
            {
                prGetHashtagFromPost.close();
            }

            if(resultHashtags != null)
            {
                resultHashtags.close();
            }
        }
    }
}
