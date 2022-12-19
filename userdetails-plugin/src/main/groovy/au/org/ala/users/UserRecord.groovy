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

package au.org.ala.users


import grails.web.databinding.WebDataBinding
import groovy.transform.EqualsAndHashCode

import java.sql.Timestamp

@EqualsAndHashCode()
//@EqualsAndHashCode(includes = 'id')
class UserRecord<T> implements WebDataBinding, Serializable {

    static hasMany =  [
            userRoles: UserRoleRecord,
            userProperties: UserPropertyRecord
    ]

    T id

    String userId

    String firstName
    String lastName

    String userName
    String email

    Date dateCreated
    Date lastUpdated

    Timestamp lastLogin

    Boolean activated
    Boolean locked

    String tempAuthKey

    String displayName

    Collection<UserRoleRecord> userRoles
    Collection<UserPropertyRecord> userProperties

    String getUiId() {
        return id?.toString() ?: ''
    }

    static constraints = {
        email nullable: true
        firstName  nullable: true
        lastName  nullable: true
        activated nullable: false
        locked nullable: false
        lastLogin nullable: true
        tempAuthKey nullable: true
        displayName nullable: true
    }

//    static List<String[]> findNameAndEmailWhereEmailIsNotNull() {
//        return UserRecord.withCriteria {
//            isNotNull('email')
//            projections {
//                property('email')
//                property('firstName')
//                property('lastName')
//            }
//        }
//    }
//
//    static List<String[]> findIdFirstAndLastName() {
//        return UserRecord.withCriteria {
//            projections {
//                property('id')
//                property('firstName')
//                property('lastName')
//            }
//        }
//    }
//
//    static List<String[]> findUserDetails() {
//        return UserRecord.withCriteria {
//            projections {
//                property('id')
//                property('firstName')
//                property('lastName')
//                property('userName')
//                property('email')
//            }
//        }
//    }

    def propsAsMap(){
        def map = [:]
        this.getUserProperties().each {
            map.put(it.name.startsWith('custom:') ? it.name.substring(7) : it.name,  it.value)
        }
        map
    }

    String toString(){
        firstName + " " + lastName + " <" +email +">"
    }
}
