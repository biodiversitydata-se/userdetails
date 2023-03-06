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

@EqualsAndHashCode
class AuthorisedSystemRecord<T> implements IAuthorisedSystem<T>, WebDataBinding, Serializable {

    T id
    T version
    String host
    String description

    static constraints = {
        host nullable: false, blank: false
        description nullable: true
    }
}
