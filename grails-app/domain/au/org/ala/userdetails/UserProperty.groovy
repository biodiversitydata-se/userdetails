package au.org.ala.userdetails

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties('metaClass')
class UserProperty implements Serializable {

    User user
    String name
    String value

    static def addOrUpdateProperty(user, name, value){

        def up = UserProperty.findByUserAndName(user, name)
        if(!up){
           up = new UserProperty(user:user, name:name, value:value)
        } else {
           up.value = value
        }
        up.save(flush:true)
        up
    }

    static mapping = {
        table 'profiles'
        id composite: ['user', 'name']
        user column:  'userid'
        name column: 'property'
        version false
        value sqlType: 'text'
    }
    static constraints = {
        value nullable: false, blank: true
        name nullable: false, blank: false
    }

    String toString(){
        name + " : " + value
    }
}
