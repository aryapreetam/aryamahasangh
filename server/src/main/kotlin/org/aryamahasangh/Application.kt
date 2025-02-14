package org.aryamahasangh

import com.expediagroup.graphql.server.ktor.*
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import org.aryamahasangh.OrganisationActivity.*

const val SERVER_PORT = 4000

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(GraphQL){
        schema {
            packages = listOf("org.aryamahasangh")
            queries = listOf(OrgsQuery())
            mutations = listOf(ConferenceMutation())
            typeHierarchy = mapOf(
                OrganisationActivity::class to listOf(Event::class, Session::class, Campaign::class)
            )
        }
    }
    install(StatusPages){
        defaultGraphQLStatusPages()
    }
    install(CORS){
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options) // Needed for preflight requests

        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHost("localhost:8080", schemes = listOf("http"))
    }
    routing {
        graphQLPostRoute()
        graphiQLRoute()
        graphQLSDLRoute()
    }
}

class OrgsQuery : Query {
    fun organisations(): List<Organisation> = listOfOrganisations
    fun organisation(name: String): Organisation? = listOfOrganisations.find { it.name == name }
    fun learning(): List<Video> = videosList
}

data class OrganisationInput(
    val name: String,
    val logo: String,
    val description: String,
    val people: List<OrganisationalMember>?
)

class ConferenceMutation : Mutation {
    fun addOrg(org: OrganisationInput): Boolean {
        return listOfOrganisations.add(
            Organisation(
                name = org.name,
                logo = org.logo,
                description = org.description,
            )
        )
    }
}