/*
 *  Copyright (C) Esaph, Julian Auguscik - All Rights Reserved
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *  * Written by Julian Auguscik <esaph.re@gmail.com>, March  2020
 *
 */

package Esaph;

import com.mysql.jdbc.PreparedStatement;

import java.sql.Connection;
import java.sql.ResultSet;

public class ServerPolicy
{
    private static final String queryCheckWatchShip = "SELECT * FROM Watcher WHERE ((UID=? AND FUID=?) OR (UID=? AND FUID=?)) AND AD=0 LIMIT 1"; //warning account delete etc checked in function.
    private static final String queryCheckIfBlocked = "SELECT * FROM BlockedUsers WHERE (UID_BLOCKER=? AND UID_BLOCKED=?) OR (UID_BLOCKER=? AND UID_BLOCKED=?) LIMIT 1";
    private static final String queryCheckIfWatchAnfrage = "SELECT * FROM WatcherAusstehend WHERE (UID=? AND FUID=?) OR (UID=? AND FUID=?) LIMIT 1";
    private static final String queryCheckFollow = "SELECT NULL FROM Following WHERE UID_FOLLOWS=? AND FUID_FOLLOWING=? AND VALID=1 LIMIT 1";

    public static final boolean POLICY_CASE_ALLOWED = true;
    public static final boolean POLICY_CASE_PERMITTED = false;



    public static final short POLICY_DETAIL_CASE_OWN = 5;
    public static final short POLICY_DETAIL_CASE_I_WAS_ANGEFRAGT = 1;
    public static final short POLICY_DETAIL_CASE_I_SENT_ANFRAGE = 2;
    public static final short POLICY_DETAIL_CASE_I_WAS_BLOCKED = 3;
    public static final short POLICY_DETAIL_CASE_I_BLOCKED_SOMEONE = 4;
    public static final short POLICY_DETAIL_CASE_FRIENDS = 6;
    public static final short POLICY_DETAIL_CASE_NOTHING = 0;


    public static short getFriendshipState(Connection connection, long ThreadUID, long FUID) // Checking friendship
    {
        PreparedStatement checkBlocked = null;
        ResultSet resultBlocked = null;
        PreparedStatement getFriends = null;
        ResultSet resultGetFriends = null;

        try
        {
            if (ThreadUID == FUID)
            {
                return ServerPolicy.POLICY_DETAIL_CASE_OWN; // OWN ACCOUNT;
            }

            checkBlocked = (PreparedStatement) connection
                    .prepareStatement(ServerPolicy.queryCheckIfBlocked);
            checkBlocked.setLong(1, ThreadUID);
            checkBlocked.setLong(2, FUID);
            checkBlocked.setLong(3, FUID);
            checkBlocked.setLong(4, ThreadUID);
            resultBlocked = checkBlocked.executeQuery();
            if (resultBlocked.next()) // BLOCKED.
            {
                if (resultBlocked.getLong("UID_BLOCKER") == ThreadUID) // Ich hab geblockt.
                {
                    resultBlocked.close();
                    checkBlocked.close();
                    return ServerPolicy.POLICY_DETAIL_CASE_I_BLOCKED_SOMEONE;
                } else // Ich wurde geblockt.
                {
                    resultBlocked.close();
                    checkBlocked.close();
                    return ServerPolicy.POLICY_DETAIL_CASE_I_WAS_BLOCKED;
                }
            }

            getFriends = (PreparedStatement) connection
                    .prepareStatement(ServerPolicy.queryCheckWatchShip);
            getFriends.setLong(1, ThreadUID);
            getFriends.setLong(2, FUID);
            getFriends.setLong(3, FUID);
            getFriends.setLong(4, ThreadUID);
            resultGetFriends = getFriends.executeQuery();

            if (resultGetFriends.next()) // We are friends.
            {
                if (resultGetFriends.getShort("WF") == 0)
                {
                    resultGetFriends.close();
                    getFriends.close();
                    return ServerPolicy.POLICY_DETAIL_CASE_FRIENDS;
                }
                else {
                    resultGetFriends.close();
                    getFriends.close();
                    return ServerPolicy.POLICY_DETAIL_CASE_NOTHING;
                }
            }
            else {
                PreparedStatement getAnfragenCheck = null;
                ResultSet resultAnfrage = null;

                try {
                    getAnfragenCheck = (PreparedStatement) connection
                            .prepareStatement(ServerPolicy.queryCheckIfWatchAnfrage);
                    getAnfragenCheck.setLong(1, ThreadUID);
                    getAnfragenCheck.setLong(2, FUID);
                    getAnfragenCheck.setLong(3, FUID);
                    getAnfragenCheck.setLong(4, ThreadUID);
                    resultAnfrage = getAnfragenCheck.executeQuery();

                    if (resultAnfrage.next())
                    {
                        if (resultAnfrage.getLong("UID") == ThreadUID)
                        {
                            return ServerPolicy.POLICY_DETAIL_CASE_I_SENT_ANFRAGE; // Ich habe jemanden eine Anfrage geschickt.
                        }
                        return ServerPolicy.POLICY_DETAIL_CASE_I_WAS_ANGEFRAGT; // Mir hat einer eine Anfrage geschickt.
                    }
                    return ServerPolicy.POLICY_DETAIL_CASE_NOTHING; // Keine Verbindungen zwischen den beiden Nutzern.
                }
                catch (Exception ec)
                {
                }
                finally
                {
                    if(getAnfragenCheck != null)
                    {
                        getAnfragenCheck.close();
                    }

                    if(resultAnfrage != null)
                    {
                        resultAnfrage.close();
                    }
                }
            }
        }
        catch (Exception ec)
        {
            return ServerPolicy.POLICY_DETAIL_CASE_NOTHING;
        }
        finally
        {
            try
            {
                if(checkBlocked != null)
                {
                    checkBlocked.close();
                }

                if(resultBlocked != null)
                {
                    resultBlocked.close();
                }

                if(getFriends != null)
                {
                    getFriends.close();
                }

                if(resultGetFriends != null)
                {
                    resultGetFriends.close();
                }
            }
            catch (Exception e)
            {
            }
        }
        return ServerPolicy.POLICY_DETAIL_CASE_NOTHING;
    }


    public static short getFriendshipStateInverted(Connection connection, long ThreadUID, long UID) //Checking friendship
    {
        try
        {
            PreparedStatement checkBlocked = (PreparedStatement) connection.prepareStatement(ServerPolicy.queryCheckIfBlocked);
            checkBlocked.setLong(1, UID);
            checkBlocked.setLong(2, ThreadUID);
            checkBlocked.setLong(3, ThreadUID);
            checkBlocked.setLong(4, UID);
            ResultSet resultBlocked = checkBlocked.executeQuery();
            if(resultBlocked.next()) //BLOCKED.
            {
                if(resultBlocked.getLong("UID_BLOCKER") == UID) //Ich hab geblockt.
                {
                    resultBlocked.close();
                    checkBlocked.close();
                    return ServerPolicy.POLICY_DETAIL_CASE_I_BLOCKED_SOMEONE;
                }
                else //Ich wurde geblockt.
                {
                    resultBlocked.close();
                    checkBlocked.close();
                    return ServerPolicy.POLICY_DETAIL_CASE_I_WAS_BLOCKED;
                }
            }

            PreparedStatement getFriends = (PreparedStatement) connection.prepareStatement(ServerPolicy.queryCheckWatchShip);
            getFriends.setLong(1, UID);
            getFriends.setLong(2, ThreadUID);
            getFriends.setLong(3, ThreadUID);
            getFriends.setLong(4, UID);
            ResultSet result = getFriends.executeQuery();


            if(result.next()) //We are friends.
            {
                if(result.getShort("WF") == 0)
                {
                    result.close();
                    getFriends.close();
                    return ServerPolicy.POLICY_DETAIL_CASE_FRIENDS;
                }
                else if(result.getShort("WF") == 1)
                {
                    result.close();
                    getFriends.close();

                    PreparedStatement getAnfragenCheck = (PreparedStatement) connection.prepareStatement(ServerPolicy.queryCheckIfWatchAnfrage);
                    getAnfragenCheck.setLong(1, UID);
                    getAnfragenCheck.setLong(2, ThreadUID);
                    getAnfragenCheck.setLong(3, ThreadUID);
                    getAnfragenCheck.setLong(4, UID);
                    ResultSet resultAnfrage = getAnfragenCheck.executeQuery();
                    if(resultAnfrage.next())
                    {
                        if(resultAnfrage.getLong("UID") == UID)
                        {
                            return ServerPolicy.POLICY_DETAIL_CASE_I_SENT_ANFRAGE; //Ich habe jemanden eine Anfrage geschickt.
                        }
                        return ServerPolicy.POLICY_DETAIL_CASE_I_WAS_ANGEFRAGT; //Mir hat einer eine Anfrage geschickt.
                    }
                    return ServerPolicy.POLICY_DETAIL_CASE_NOTHING; //Keine Verbindungen zwischen den beiden Nutzern.
                }
            }
            else
            {
                PreparedStatement getAnfragenCheck = (PreparedStatement) connection.prepareStatement(ServerPolicy.queryCheckIfWatchAnfrage);
                getAnfragenCheck.setLong(1, UID);
                getAnfragenCheck.setLong(2, ThreadUID);
                getAnfragenCheck.setLong(3, ThreadUID);
                getAnfragenCheck.setLong(4, UID);
                ResultSet resultAnfrage = getAnfragenCheck.executeQuery();
                if(resultAnfrage.next())
                {
                    if(resultAnfrage.getLong("UID") == UID)
                    {
                        return ServerPolicy.POLICY_DETAIL_CASE_I_SENT_ANFRAGE; //Ich habe jemanden eine Anfrage geschickt.
                    }
                    return ServerPolicy.POLICY_DETAIL_CASE_I_WAS_ANGEFRAGT; //Mir hat einer eine Anfrage geschickt.
                }
                return ServerPolicy.POLICY_DETAIL_CASE_NOTHING; //Keine Verbindungen zwischen den beiden Nutzern.
            }

            return ServerPolicy.POLICY_DETAIL_CASE_NOTHING;
        }
        catch(Exception ec)
        {
            return ServerPolicy.POLICY_DETAIL_CASE_NOTHING;
        }
    }


    private static final String queryCheckPermission = "SELECT NULL FROM Watcher WHERE ((UID=? AND FUID=?) OR (UID=? AND FUID=?)) AND AD=0 AND WF=0 LIMIT 1"; //warning account delete etc checked in function.
    public static boolean isAllowed(Connection connection, long ThreadUID, long FUID) // Checking friendship
    {
        PreparedStatement checkBlocked = null;
        ResultSet resultBlocked = null;
        PreparedStatement getFriends = null;
        ResultSet resultGetFriends = null;

        PreparedStatement preparedStatementCheckFollowing = null;
        ResultSet resultSetCheckFollowing = null;

        try
        {
            if (ThreadUID == FUID)
            {
                return ServerPolicy.POLICY_CASE_ALLOWED; // OWN ACCOUNT;
            }

            checkBlocked = (PreparedStatement) connection
                    .prepareStatement(ServerPolicy.queryCheckIfBlocked);
            checkBlocked.setLong(1, ThreadUID);
            checkBlocked.setLong(2, FUID);
            checkBlocked.setLong(3, FUID);
            checkBlocked.setLong(4, ThreadUID);
            resultBlocked = checkBlocked.executeQuery();
            if (resultBlocked.next()) // BLOCKED.
            {
                return ServerPolicy.POLICY_CASE_PERMITTED;
            }

            getFriends = (PreparedStatement) connection
                    .prepareStatement(ServerPolicy.queryCheckPermission);
            getFriends.setLong(1, ThreadUID);
            getFriends.setLong(2, FUID);
            getFriends.setLong(3, FUID);
            getFriends.setLong(4, ThreadUID);
            resultGetFriends = getFriends.executeQuery();

            if (resultGetFriends.next()) // We are friends.
            {
                return ServerPolicy.POLICY_CASE_ALLOWED;
            }
            else //No friends, so check for other
            {
                preparedStatementCheckFollowing = (PreparedStatement) connection
                        .prepareStatement(ServerPolicy.queryCheckFollow);
                preparedStatementCheckFollowing.setLong(1, ThreadUID);
                preparedStatementCheckFollowing.setLong(2, FUID);
                resultSetCheckFollowing = preparedStatementCheckFollowing.executeQuery();

                if(resultSetCheckFollowing.next())
                {

                }
            }
        }
        catch (Exception ec)
        {
            return ServerPolicy.POLICY_CASE_PERMITTED;
        }
        finally
        {
            try
            {
                if(checkBlocked != null)
                {
                    checkBlocked.close();
                }

                if(resultBlocked != null)
                {
                    resultBlocked.close();
                }

                if(getFriends != null)
                {
                    getFriends.close();
                }

                if(resultGetFriends != null)
                {
                    resultGetFriends.close();
                }
            }
            catch (Exception e)
            {
            }
        }
        return ServerPolicy.POLICY_CASE_PERMITTED;
    }


}
