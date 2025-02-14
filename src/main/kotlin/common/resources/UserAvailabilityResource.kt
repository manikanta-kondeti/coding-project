package common.resources

import kotlinx.coroutines.*
import common.core.AvailabilityDTO
import common.core.AvailabilityDTOTransformer
import common.core.mappers.UserAvailability
import common.core.service.UserAvailabilityService
import common.core.service.UserService
import common.resources.DTO.UserAvailabilityDTO
import java.time.Instant
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/*
    this resource holds all the API methods exposed to the frontend
    objective: as a rule of thumb, we want to transform the datetime object in the
        presentation layer, therefore, backend only stores and forwards UTC epoch

     REST API:
        - getAvailabilityByUser
            userId

        - getOverlappingAvailability
            userId1
            userId2
            => compares UTC timestamps and expose common timestamps in UTC

        - createAvailability
            {userId1
                [
                    {date, startTime, endTime},
                    {date, startTime, endTime}
                ]
            }
 */
@Path("/availability")
class UserAvailabilityResource @Inject constructor(
    private val userAvailabilityService: UserAvailabilityService,
    private val userService: UserService
) {

    @GET
    @Path("/user/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAvailabilityByUser(@PathParam("id") userId: Int): Response {
        val data = runBlocking {  userAvailabilityService.getUserAvailability(userId) }
        val avaiabilityOfUser = mutableListOf<AvailabilityDTO>()
        data!!.forEach {
            avaiabilityOfUser.add(AvailabilityDTOTransformer.toDTO(it))
        }
        return Response.ok(avaiabilityOfUser).build()
    }

    @POST
    @Path("/userSubmitAvailability/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun submitAvailability(@PathParam("userId") userId: Int, userAvailabilityObject: UserAvailabilityDTO): Response {
        // validations
        userService.getUserById(userId) ?: return Response.status(400).build()
        userAvailabilityObject.availabilityList.forEach {
            userAvailabilityService.createUserAvailability(
                UserAvailability(
                    userId = userId,
                    date = it.date.toLong(),
                    startTime = it.startTime.toLong(),
                    endTime = it.endTime.toLong()
                )
            )
        }
        return Response.ok().build()
    }

    @GET
    @Path("/userAvailability/getOverlappingAvailability/user1/{userId1}/userId2/{userId2}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getOverlappingAvailability(@PathParam("userId1") userId1: String, @PathParam("userId2") userId2: String, @DefaultValue("0") @QueryParam("date")  date: String): Response {
        userService.getUserById(userId1.toInt()) ?: return Response.status(400).build()
        userService.getUserById(userId2.toInt()) ?: return Response.status(400).build()

        var outlist: List<UserAvailability>? = null
        val dateConvertedToEpochMilli = if (date.toLong() > 0){date.toLong()} else {Instant.now().toEpochMilli()}
        outlist = userAvailabilityService.getOverlappingAvailability(userId1.toInt(), userId2.toInt(), dateConvertedToEpochMilli)
        return Response.ok(outlist).build()
    }

}