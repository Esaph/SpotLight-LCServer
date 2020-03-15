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

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DeleteLifeCloudPost extends EsaphCommandLC
{
    private static final String QUERY_GET_PATH = "SELECT PATH FROM LifeCloud WHERE UID=? AND PID=?";
    private static final String QUERY_DELETE_LIFECLOUDPOST = "DELETE FROM LifeCloud WHERE UID=? AND PID=?";

    public DeleteLifeCloudPost(LifeCloudServer lifeCloudServer, LifeCloudServer.RequestHandler requestHandler, LogUtilsEsaph logUtilsRequest)
    {
        super(lifeCloudServer, requestHandler, logUtilsRequest);
    }

    @Override
    public void run() throws Exception
    {
        PreparedStatement preparedStatementGetPath = null;
        ResultSet resultSet = null;

        try
        {
            String PID = super.requestHandler().getJSONMessage().getString("PID");
            preparedStatementGetPath = (PreparedStatement) super.requestHandler().getCurrentConnectionToSql().prepareStatement(DeleteLifeCloudPost.QUERY_GET_PATH);
            preparedStatementGetPath.setLong(1, super.requestHandler().getThreadUID());
            preparedStatementGetPath.setString(2, PID);

            resultSet = preparedStatementGetPath.executeQuery();
            if(resultSet.next())
            {
                File file = new File(resultSet.getString("PATH"));
                if(file.delete())
                {
                    PreparedStatement preparedStatement = (PreparedStatement) super.requestHandler().getCurrentConnectionToSql().prepareStatement(DeleteLifeCloudPost.QUERY_DELETE_LIFECLOUDPOST);
                    preparedStatement.setLong(1, super.requestHandler().getThreadUID());
                    preparedStatement.setString(2, PID);
                    int result = preparedStatement.executeUpdate();
                    preparedStatement.close();

                    if(result > 0)
                    {
                        super.requestHandler().getWriter().println("1");
                    }
                    else
                    {
                        super.requestHandler().getWriter().println("0");
                    }
                }
                else
                {
                    super.requestHandler().getWriter().println("0");
                }
            }
            else
            {
                super.requestHandler().getWriter().println("0");
            }
        }
        catch (Exception ec)
        {

        }
        finally
        {
            if(preparedStatementGetPath != null)
            {
                preparedStatementGetPath.close();
            }

            if(resultSet != null)
            {
                resultSet.close();
            }
        }
    }
}
