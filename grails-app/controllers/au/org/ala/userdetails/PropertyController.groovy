package au.org.ala.userdetails

import au.org.ala.auth.PreAuthorise
import grails.converters.JSON
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

import javax.ws.rs.Path
import javax.ws.rs.Produces

import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY

@Path("property")
class PropertyController extends BaseController {

    def profileService
    def authorisedSystemService

    static allowedMethods = [getProperty: "GET", saveProperty: "POST"]

    def index() {}

    /**
     * get a property value for a user
     * @return
     */
    @Operation(
            method = "GET",
            tags = "properties",
            summary = "Get Property",
            operationId = "getProperty",
            description = "Get a property value for a user",
            parameters = [
                    @Parameter(
                            name = "alaId",
                            in = QUERY,
                            description = "The user's ALA ID",
                            schema = @Schema(implementation = Long),
                            required = true
                    ),
                    @Parameter(
                            name = "name",
                            in = QUERY,
                            description = "The name of the property to get",
                            required = true
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Successful get property request",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            array = @ArraySchema(schema = @Schema(implementation = UserProperty))
                                    )
                            ]
                    )
            ],
            security = [@SecurityRequirement(scopes = ['read:userdetails'])]
    )
    @Path("getProperty")
    @Produces("application/json")
    @PreAuthorise(requiredScope = 'read:userdetails')
    def getProperty() {
        String name = params.name
        Long alaId = params.long('alaId')
        if (!name || !alaId) {
            badRequest "name and alaId must be provided";
        } else {
            User user = User.findById(alaId);
            List props
            if (user) {
                props = profileService.getUserProperty(user, name);
                render text: props as JSON, contentType: 'application/json'
            } else {
                notFound "Could not find user for id: ${alaId}";
            }
        }
    }

    /**
     * save a property value to a user
     * @return
     */
    @Operation(
            method = "POST",
            tags = "properties",
            summary = "Save a Property",
            operationId = "saveProperty",
            description = "Saves a property value for a user",
            parameters = [
                    @Parameter(
                            name = "alaId",
                            in = QUERY,
                            description = "The user's ALA ID",
                            schema = @Schema(implementation = Long),
                            required = true
                    ),
                    @Parameter(
                            name = "name",
                            in = QUERY,
                            description = "The name of the property to set",
                            required = true
                    ),
                    @Parameter(
                            name = "value",
                            in = QUERY,
                            description = "The value of the property to set.",
                            required = false
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Successful save property request",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = UserProperty)
                                    )
                            ]
                    ),
                    @ApiResponse(
                            description = "Could not find user",
                            responseCode = "404",
                            content = [@Content(mediaType = "text/plain")]
                    ),
            ],
            security = [@SecurityRequirement(scopes = ['write:userdetails'])]
    )
    @Path("saveProperty")
    @Produces("application/json")
    @PreAuthorise(requiredScope = 'write:userdetails')
    def saveProperty(){
        String name = params.name;
        String value = params.value;
        Long alaId = params.long('alaId');
        if (!name || !alaId) {
            badRequest "name and alaId must be provided";
        } else {
            User user = User.findById(alaId);
            UserProperty property
            if (user) {
                property = profileService.saveUserProperty(user, name, value);
                if (property.hasErrors()) {
                    saveFailed()
                } else {
                    render text: property as JSON, contentType: 'application/json'
                }
            } else {
                notFound "Could not find user for id: ${alaId}";
            }
        }

    }
}
