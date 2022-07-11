/**
 * Copyright 2022 Gautam Satpathy
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package in.satpathy.charanna.broker.zerodha;

/*
 *  Imports
 */
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *  Only logs the event to the logger.
 */
public class CharannaSessionExpiryHook implements SessionExpiryHook  {

    private static final Logger logger = LogManager.getLogger( CharannaSessionExpiryHook.class ) ;

    /**
     *  Default Constructor.
     */
    public CharannaSessionExpiryHook() {
    }

    @Override
    public void sessionExpired() {
        logger.info( "KiteConnect Session Expired." ) ;
    }

}   /*  End of the CharannaSessionExpiryHook class. */
