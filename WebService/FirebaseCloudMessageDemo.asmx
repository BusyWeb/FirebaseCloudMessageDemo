<%@ WebService Language="C#" Class="FirebaseCloudMessageDemo" %>

using System;
using System.Web;
using System.Web.Services;
using System.Web.Services.Protocols;
using System.Data;
using System.Data.SqlClient;
using System.Collections.Generic;
using System.Web.Script.Serialization;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading;

[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
// To allow this Web Service to be called from script, using ASP.NET AJAX, uncomment the following line. 
[System.Web.Script.Services.ScriptService]
public class FirebaseCloudMessageDemo : System.Web.Services.WebService
{

    [WebMethod]
    public string HelloWorld()
    {
        return "Hello World";
    }

    private string GetConnectionString()
    {
        // your database connection string here: usually from web.config
        return System.Web.Configuration.WebConfigurationManager.ConnectionStrings["production"].ConnectionString;
    }

    [WebMethod(EnableSession = true)]
    public string GetAllUsers()
    {
        string result = "";

        try
        {
            List<DataHelper.FcmUser> users = DataHelper.GetAllUsers(GetConnectionString());
            if (users == null || users.Count < 1)
            {
                return String.Empty;
            }

            JavaScriptSerializer serializer = new JavaScriptSerializer();
            result = serializer.Serialize(users);
            Context.Response.Output.Write(result);
            //Context.Response.End();
            Context.Response.Flush();
            Context.Response.SuppressContent = true;
            Context.ApplicationInstance.CompleteRequest();
        }
        catch (Exception e)
        {
        }

        return String.Empty;
    }

    [WebMethod(EnableSession = true)]
    public string GetUser(string email)
    {
        string result = "";

        try
        {
            DataHelper.FcmUser user = DataHelper.GetUser(GetConnectionString(), email);
            if (user == null)
            {
                return String.Empty;
            }

            JavaScriptSerializer serializer = new JavaScriptSerializer();
            result = serializer.Serialize(user);
            Context.Response.Output.Write(result);
            //Context.Response.End();
            Context.Response.Flush();
            Context.Response.SuppressContent = true;
            Context.ApplicationInstance.CompleteRequest();
        }
        catch (Exception e)
        {
        }

        return String.Empty;
    }

    [WebMethod(EnableSession = true)]
    public string RegisterUser(string email, string uid, string deviceToken)
    {
        String result = "";

        try
        {
            bool success = DataHelper.RegisterUser(GetConnectionString(), email, uid, deviceToken);
            if (!success)
            {
                return String.Empty;
            }

            DataHelper.FcmUser user = DataHelper.GetUser(GetConnectionString(), email);
            JavaScriptSerializer serializer = new JavaScriptSerializer();
            result = serializer.Serialize(user);
            Context.Response.Output.Write(result);
            //Context.Response.End();
            Context.Response.Flush();
            Context.Response.SuppressContent = true;
            Context.ApplicationInstance.CompleteRequest();
        }
        catch (Exception e)
        {
        }

        return String.Empty;
    }

    [WebMethod(EnableSession = true)]
    public string UnRegisterUser(string email)
    {
        String result = "";

        try
        {
            bool success = DataHelper.UnRegisterUser(GetConnectionString(), email);
            if (!success)
            {
                return String.Empty;
            }

            JavaScriptSerializer serializer = new JavaScriptSerializer();
            result = serializer.Serialize("OK");
            Context.Response.Output.Write(result);
            //Context.Response.End();
            Context.Response.Flush();
            Context.Response.SuppressContent = true;
            Context.ApplicationInstance.CompleteRequest();
        }
        catch (Exception e)
        {
        }

        return String.Empty;
    }

    [WebMethod(EnableSession = true)]
    public string SendRemoteMessage(string email, string message)
    {
        string result = "";

        try
        {
            DataHelper.FcmUser user = DataHelper.GetUser(GetConnectionString(), email);
            if (user == null || user.Id < 0)
            {
                return String.Empty;
            }

            string resultResponse = FcmHelper.SendRemoteMessage(user.DeviceToken, message);
            // response >> Json
            Context.Response.Output.Write(resultResponse);
            //Context.Response.End();
            Context.Response.Flush();
            Context.Response.SuppressContent = true;
            Context.ApplicationInstance.CompleteRequest();
        }
        catch (Exception e)
        {
            //return e.ToString();
            Thread.ResetAbort();
        }

        return String.Empty;
    }




    // For production environment, recommend to use DAL(Data Access Layer) and BI
    // Database: recommend to use Stored Procedures
    public class DataHelper
    {

        public class FcmUser
        {
            public int Id { get; set; }
            public string Email { get; set; }
            public string UserId { get; set; }
            public string DeviceToken { get; set; }
        }

        public static List<FcmUser> GetAllUsers(String connectionString)
        {
            List<FcmUser> users = new List<FcmUser>();
            String commandText = "Select * From FCMDemoUsers";

            using (SqlConnection conn = new SqlConnection(connectionString))
            {
                conn.Open();

                using (SqlCommand cmd = new SqlCommand(commandText, conn))
                {
                    cmd.CommandType = System.Data.CommandType.Text;
                    cmd.CommandTimeout = 1800;

                    DataSet ds = new DataSet();
                    using (SqlDataAdapter da = new SqlDataAdapter(cmd))
                    {
                        da.Fill(ds);
                        if (ds.Tables.Count > 0)
                        {
                            DataTable dt = ds.Tables[0];
                            foreach (DataRow dr in dt.Rows)
                            {
                                FcmUser user = new FcmUser
                                {
                                    Id = Convert.ToInt32(dr["Id"]),
                                    Email = dr["Email"].ToString(),
                                    UserId = dr["UserId"].ToString(),
                                    DeviceToken = dr["DeviceToken"].ToString()
                                };
                                users.Add(user);
                            }
                        }
                    }

                }
            };
            return users;
        }

        public static FcmUser GetUser(String connectionString, String email)
        {
            FcmUser user = null;
            String commandText = "Select * From FCMDemoUsers Where Email='" + email + "'";

            using (SqlConnection conn = new SqlConnection(connectionString))
            {
                conn.Open();

                using (SqlCommand cmd = new SqlCommand(commandText, conn))
                {
                    cmd.CommandType = System.Data.CommandType.Text;
                    cmd.CommandTimeout = 1800;

                    DataSet ds = new DataSet();
                    using (SqlDataAdapter da = new SqlDataAdapter(cmd))
                    {
                        da.Fill(ds);
                        if (ds.Tables.Count > 0)
                        {
                            DataTable dt = ds.Tables[0];
                            DataRow dr = dt.Rows[0];
                            user = new FcmUser
                            {
                                Id = Convert.ToInt32(dr["Id"]),
                                Email = dr["Email"].ToString(),
                                UserId = dr["UserId"].ToString(),
                                DeviceToken = dr["DeviceToken"].ToString()
                            };
                        }
                    }

                }
            };
            return user;
        }

        public static bool RegisterUser(String connectionString, String email, String uid, String deviceToken)
        {
            bool result = false; ;
            String commandText = "Insert Into FCMDemoUsers(Email, UserId, DeviceToken) Values('"
                    + email + "','"
                    + uid + "','"
                    + deviceToken + "')";

            using (SqlConnection conn = new SqlConnection(connectionString))
            {
                conn.Open();

                using (SqlCommand cmd = new SqlCommand(commandText, conn))
                {
                    cmd.CommandType = System.Data.CommandType.Text;
                    cmd.CommandTimeout = 1800;

                    cmd.ExecuteNonQuery();

                    result = true;
                }
            };
            return result;
        }

        public static bool UnRegisterUser(String connectionString, String email)
        {
            bool result = false;
            String commandText = "Delete From FCMDemoUsers Where Email='"
                    + email + "'";

            using (SqlConnection conn = new SqlConnection(connectionString))
            {
                conn.Open();

                using (SqlCommand cmd = new SqlCommand(commandText, conn))
                {
                    cmd.CommandType = System.Data.CommandType.Text;
                    cmd.CommandTimeout = 1800;

                    cmd.ExecuteNonQuery();

                    result = true;
                }
            };
            return result;
        }

    }

    // Recommand to use separate layer (BI or Data)
    public class FcmHelper
    {
        public static string FcmAuthorization = "your-firebase-cloud-messaging-server-key";

        public class MessageResponse
        {
            public string multicast_id;
            public string success;
            public string failure;
            public string canonical_ids;
        }
        public class MessageData
        {
            public string From;
            public string Message;
        }
        public class MessageObject
        {
            public MessageData data;
            public String to;
        }

        public static String SendRemoteMessage(string sendTo, string message = "")
        {
            String responseString = "";
            bool success = false;
            try
            {
                string Authorization = FcmAuthorization;
                string FcmUrl = "https://fcm.googleapis.com/fcm/send";

                WebRequest tRequest;
                tRequest = WebRequest.Create(FcmUrl);
                tRequest.Method = "post";
                tRequest.ContentType = "application/json;charset=UTF-8";
                tRequest.Headers.Add(string.Format("Authorization: key={0}", Authorization));
                //tRequest.Headers.Add(string.Format("Content-Type: {0}", "application/json"));

                string postData = "";
                var objData = new MessageObject
                {
                    data = new MessageData
                    {
                        From = "Firebase Cloud Message Demo",
                        Message = message
                    },
                    to = sendTo
                };
                postData = new System.Web.Script.Serialization.JavaScriptSerializer().Serialize(objData);

                //Console.WriteLine(postData);

                Byte[] byteArray = Encoding.UTF8.GetBytes(postData);
                tRequest.ContentLength = byteArray.Length;

                Stream dataStream = tRequest.GetRequestStream();
                dataStream.Write(byteArray, 0, byteArray.Length);
                dataStream.Close();

                HttpWebResponse response = (HttpWebResponse)tRequest.GetResponse();
                HttpStatusCode status = response.StatusCode;
                if (status == HttpStatusCode.OK)
                {
                    success = true;

                    using (Stream stream = response.GetResponseStream())
                    {
                        StreamReader reader = new StreamReader(stream, Encoding.UTF8);
                        responseString = reader.ReadToEnd();
                    }
                }
                else
                {
                    success = false;
                }
            }
            catch
            {
                success = false;

            }

            return responseString;
        }
    }

}