package org.aryamahasangh

import com.expediagroup.graphql.generator.hooks.FlowSubscriptionSchemaGeneratorHooks
import com.expediagroup.graphql.generator.scalars.ID
import com.expediagroup.graphql.server.ktor.*
import com.expediagroup.graphql.server.ktor.subscriptions.KtorGraphQLSubscriptionHooks
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.datetime.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val SERVER_PORT = 4000

fun main() {
  embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
    .start(wait = true)
}

fun Application.module() {
  install(GraphQL) {
    schema {
      packages = listOf("org.aryamahasangh")
      queries = listOf(OrgsQuery())
      mutations = listOf(
        OrgsMutation(),
        ActivityMutation()
      )
      subscriptions = listOf(
        OrgSubscriptionService()
      )
      hooks = CustomSchemaGeneratorHooks()
    }
    server {
      contextFactory = DefaultKtorGraphQLContextFactory()
      subscriptions {
        hooks = CustomGraphqlSubscriptionHooks()
      }
    }
  }
  install(WebSockets) {
    //pingPeriod = 1.seconds
    contentConverter = JacksonWebsocketContentConverter()
  }
  install(StatusPages) {
    defaultGraphQLStatusPages()
  }
  install(CORS) {
    allowHeader(HttpHeaders.AccessControlAllowOrigin)
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Options) // Needed for preflight requests

    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.Accept)
    //allowHost("localhost:8080", schemes = listOf("http"))
    anyHost()
  }
  routing {
    graphQLPostRoute()
    graphiQLRoute()
    graphQLSDLRoute()
    graphQLSubscriptionsRoute()
  }
}

class CustomGraphqlSubscriptionHooks : KtorGraphQLSubscriptionHooks {
  override fun onConnect(
    connectionParams: Any?,
    session: WebSocketServerSession,
    graphQLContext: GraphQLContext
  ): GraphQLContext {
    println("onConnect ${session}")
    return super.onConnect(connectionParams, session, graphQLContext)
  }
}

@OptIn(ExperimentalUuidApi::class)
class OrgsQuery : Query {
  fun organisations(): List<Organisation> = listOfOrganisations
  fun organisation(name: String): Organisation? = listOfOrganisations.find { it.name == name }
  fun organisationalActivities(): List<OrganisationalActivity> = activities
  fun organisationalActivity(id: ID) =  activities.find { it.id == id }
  fun learningItems(): List<Video> = videosList
  fun learningItem(id: ID) = videosList.find { it.id == id }
}

data class OrganisationInput(
  val name: String,
  val logo: String,
  val description: String,
  val people: List<OrganisationalMember>
)

@OptIn(ExperimentalUuidApi::class)
class OrgsMutation : Mutation {
  fun addOrganisation(input: OrganisationInput): Boolean {
    val org = Organisation(
      name = input.name,
      logo = input.logo,
      description = input.description,
      keyPeople = input.people
    )
    OrganisationPublisher.publishOrganisation(org)
    return listOfOrganisations.add(org)
  }
  fun addMemberToOrganisation(orgId: Uuid, orgMember: OrganisationalMember): Boolean {
    return false
  }

  fun removeMemberFromOrganisation(orgId: Uuid, memberId: Uuid): Boolean {
    return false
  }

  fun updateOrganisationMember(orgId: Uuid, memberId: Uuid, orgMemberDetails: OrganisationalMember): Boolean {
    return false
  }

  fun updateOrganisationDetails(orgId: Uuid, name: String, description: String, logo: String): Boolean {
    return false
  }

  fun removeOrganisation(orgId: Uuid): Boolean {
    return false
  }
}

@OptIn(ExperimentalUuidApi::class)
class ActivityMutation : Mutation {
  fun addActivity(activity: OrganisationalActivity): Boolean {
    return false
  }
  fun removeActivity(activityId: Uuid): Boolean {
    return false
  }
  fun updateActivity(activity: OrganisationalActivity): Boolean {
    return false
  }
}

//class CustomSchemaGeneratorHooks : SchemaGeneratorHooks {
//  @OptIn(ExperimentalUuidApi::class)
//  override fun willGenerateGraphQLType(type: KType): GraphQLType? {
//    return when(type.classifier as? KClass<*>){
//      Uuid::class -> graphqlUuidType
//      LocalDateTime::class -> LocalDateTimeScalar.instance
//      else -> super.willGenerateGraphQLType(type)
//    }
//  }
//}

class CustomSchemaGeneratorHooks : FlowSubscriptionSchemaGeneratorHooks() {
  @OptIn(ExperimentalUuidApi::class)
  override fun willGenerateGraphQLType(type: KType): GraphQLType? {
    return when(type.classifier as? KClass<*>){
      Uuid::class -> graphqlUuidType
      LocalDateTime::class -> LocalDateTimeScalar.instance
      else -> super.willGenerateGraphQLType(type)
    }
  }
}

val graphqlUuidType = GraphQLScalarType.newScalar()
  .name("Uuid")
  .description("A type representing a formatted kotlin uuid")
  .coercing(UuidCoercing)
  .build()

@OptIn(ExperimentalUuidApi::class)
object UuidCoercing : Coercing<Uuid, String> {
  override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): Uuid? {
    return Uuid.parse(input as String)
  }

  override fun parseLiteral(
    input: Value<*>,
    variables: CoercedVariables,
    graphQLContext: GraphQLContext,
    locale: Locale
  ): Uuid? {
    val uuidString = (input as? StringValue)?.value
    return Uuid.parse(uuidString!!)
  }

  override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String? {
    return dataFetcherResult.toString()
  }
}

object LocalDateTimeScalar {
  val instance: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("LocalDateTime")
    .description("A scalar representing kotlinx.datetime.LocalDateTime")
    .coercing(object : Coercing<LocalDateTime, String> {
      override fun serialize(dataFetcherResult: Any): String {
        return (dataFetcherResult as LocalDateTime).toString() // ISO-8601 format
      }

      override fun parseValue(input: Any): LocalDateTime {
        return LocalDateTime.parse(input as String) // Assumes ISO-8601 format
      }

      override fun parseLiteral(input: Any): LocalDateTime {
        return parseValue(input)
      }
    })
    .build()
}