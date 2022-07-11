# Auto Login to Zerodha's Kite Connect API

Automatic log in to Zerodha's Kite Connect API.
 
Needs: API Key, API Secret, (Zerodha) User ID, User Password, 2FA PIN and Redirect URL.

Will look for a text file named "access.token.txt" in the folder from which it is run. If one is found the access_token will be read from it, and a connection established. If the text file is not found then it will access the Kite Login URL, fill out the  Web Page form with the provided User ID & Password and submit the form. The 2FA PIN will be submitted in the response page and response_token retrieved to log into Kite Connect API servers. The Access Token will be written to file for later use.

If the access token is in the text file but is no longer valid, then the same process will be used to automatically log in and generate a new Access Token, which will then be written to file for later use.

After successful log in, the KiteConnect object available from this class will be authorized and can be used to communicate with the Kite Connect API Servers.

    // Create a KiteLogin object
    KiteLogin kLogin = new KiteLogin( "KITE-USER-ID", "KITE-PASSWORD", "KITE-2FA-PIN", "API-KEY", "API-SECRET", "APP-REDIRECT-URL") ;
    
    // Get the KiteConnect object and use it
    KiteConnect kiteConnect = kLogin.getKiteConnect() ;

Copyright 2022 Gautam Satpathy

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
