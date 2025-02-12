package org.aryamahasangh

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSDLRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.aryamahasangh.OrganisationActivity.*

const val SERVER_PORT = 8080

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
    routing {
        graphQLPostRoute()
        graphiQLRoute()
        graphQLSDLRoute()
    }
}

class OrgsQuery : Query {
    fun orgs(): List<Organisation> = listOfOrganisations
    fun org(name: String): Organisation? = listOfOrganisations.find { it.name == name }
    fun videos(): List<Video> = videosList
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