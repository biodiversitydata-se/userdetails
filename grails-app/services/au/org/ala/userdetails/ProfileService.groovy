/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.userdetails

import au.org.ala.users.User
import au.org.ala.users.UserProperty

class ProfileService {

    List getUserProperty(User user, String name) {
        UserProperty.findAllByUserAndName(user, name)?:[];
    }

    UserProperty saveUserProperty(User user, String name, String value){
        UserProperty.addOrUpdateProperty(user, name, value);
    }

    List getAllAvailableProperties() {
        UserProperty.withCriteria {
            projections {
                distinct("name")
            }
            order("name")
        }
    }
}
