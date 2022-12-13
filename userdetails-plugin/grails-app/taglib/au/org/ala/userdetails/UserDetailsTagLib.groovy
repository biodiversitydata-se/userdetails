package au.org.ala.userdetails

import org.springframework.beans.factory.annotation.Autowired

class UserDetailsTagLib {

    static namespace = 'ud'

    static defaultEncodeAs = [taglib:'html']
    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]
    static returnObjectForTags = ['roleList', 'userList']

    @Autowired
    IUserService userService

    def roleList = { attrs, body ->
        userService.listRoles()
    }

    def userList = { attrs, body ->
        userService.listUsers()
    }

}
