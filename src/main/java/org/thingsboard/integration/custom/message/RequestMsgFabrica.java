/**
 * Copyright © 2020-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.custom.message;

public class RequestMsgFabrica  {

    public static RequestMsg getRequestMsg(String typeDevice) {
        if (typeDevice.equals("teltonika")) {
            return new TeltonikaRequestMsg();
        } else if (typeDevice.equals("galileosky")) {
            return new GalileoskyRequestMsg();
        }
        return null;
    }

}
