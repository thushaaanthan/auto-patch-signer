//
// Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//


import ballerina.net.http;
import ballerina.data.sql;
import ballerina.io;
import ballerina.log;
import ballerina.config;
import ballerina.util;

const string keystorePath = config:getGlobalValue("KEY_STORE_PATH");
const string keystorePassword = config:getGlobalValue("KEY_STORE_PASSWORD");
const string certificatePassword = config:getGlobalValue("CERT_PASSWORD");
const string serviceUsername = config:getGlobalValue("SERVICE_USERNAME");
const string servicePassword = config:getGlobalValue("SERVICE_PASSWORD");
const int httpsPort = getPortNumber();

string programStatus = "No results yet...";
boolean endProcess = false;

@http:configuration {basePath:"/productName",httpsPort:httpsPort, keyStoreFile: keystorePath, keyStorePassword:keystorePassword, certPassword: certificatePassword}

service<http> productName {

    @http:resourceConfig {
        methods:["GET"],
        path:"/fetchData"
    }
    resource fetchData (http:Connection conn, http:InRequest request) {
        endpoint<sql:ClientConnector> sqlClient {
            create sql:ClientConnector(
            sql:DB.MYSQL, "192.168.56.212", 3306, ("wumlivesync" + "?useSSL=false"), "root", "password", {maximumPoolSize:5});  //203.94.95.218
        }
        http:OutResponse response = {};
        programStatus = "processing";

        io:println(request);
        boolean auth = authenticate(request);
        if(auth) {
            table OpenPrTable = sqlClient.select("SELECT PRODUCT_NAME AS value, PRODUCT_VERSION FROM PRODUCT_DETAILS ORDER BY PRODUCT_NAME ASC", null, null);
            var jsonOpenPrArray, _ = <json>OpenPrTable;
            json[] result = [];
            int i = 0;


            foreach item in jsonOpenPrArray {
                json temp = {};
                var temp1, _ = (string)item["PRODUCT_VERSION"];
                var temp2, _ = (string)item["value"];
                temp["value"] = temp2 + " " + temp1;
                temp["label"] = temp["value"];
                result[i] = temp;
                i = i + 1;

            }

            response.addHeader("Access-Control-Allow-Origin", "*");
            response.setJsonPayload(result);
            _ = conn.respond(response);
            sqlClient.close();
        }else{
            log:printError("Basic Authorization Failed");
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.setJsonPayload([]);
            _ = conn.respond(response);
        }

    }

    //end the validate process

    //@http:resourceConfig {
    //    methods:["POST"],
    //    path:"/endProcess"
    //}
    //resource endProcess (http:Connection conn, http:InRequest request) {
    //    string endStr = request.getStringPayload();
    //    io:println(request);
    //    io:println("endProcess received: " + endStr);
    //    programStatus = endStr;
    //    endProcess = true;
    //
    //    http:OutResponse response = {};
    //    _ = conn.respond(response);
    //
    //}
    //
    ////show the text display
    //
    //@http:resourceConfig {
    //    methods:["GET"],
    //    path:"/showResult"
    //}
    //resource showResult (http:Connection conn, http:InRequest request) {
    //    string resultStr = request.getStringPayload();
    //    io:println(resultStr);
    //    http:OutResponse response = {};
    //    while(!endProcess) {
    //
    //    }
    //    endProcess = false;
    //    json[] result= [{"result":programStatus}];
    //    response.addHeader("Access-Control-Allow-Origin", "*");
    //    response.setJsonPayload(result);
    //    programStatus = "No results yet...";
    //    _ = conn.respond(response);
    //}

}

function getPortNumber()(int){
    var portNumber , _ = <int> config:getGlobalValue("HTTPS_PORT");
    return portNumber;
}

function authenticate(http:InRequest inRequest)(boolean){
    boolean result = false;
    var basicAuthHeader = inRequest.getHeader("Authorization");
    if (basicAuthHeader == null) {
        io:println("401: No Authentication header found in the request");
        return result;
    }
    if(basicAuthHeader.hasPrefix("Basic")){
        string authHeaderValue = basicAuthHeader.subString(5, basicAuthHeader.length()).trim();
        string decodedBasicAuthHeader = util:base64Decode(authHeaderValue);
        string[] decodedCredentials = decodedBasicAuthHeader.split(":");
        if(lengthof decodedCredentials != 2){
            io:println("401: Incorrect Basic Authentication header");
        }
        else{
            if(decodedCredentials[0] == serviceUsername && decodedCredentials[1] == servicePassword){
                result = true;
            }
            else{
                io:println("Error in credentials");
            }
        }
    }
    else{
        io:println("Unsupported authentication scheme, only Basic auth is supported");
    }
    return result;
}
