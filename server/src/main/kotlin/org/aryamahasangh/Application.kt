package org.aryamahasangh

import com.expediagroup.graphql.generator.hooks.FlowSubscriptionSchemaGeneratorHooks
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
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val SERVER_PORT = System.getenv("PORT")?.toIntOrNull() ?: 4000

val url = "https://placeholder-staging-supabase.co"
val key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ0bnd3aXdtbGpjd3pwc2F3ZG1mIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzQ5MzE4OTMsImV4cCI6MjA1MDUwNzg5M30.cY4A4ZxqHA_1VRC-k6URVAHHkweHTR8FEYEzHYiu19A"
val supabase =  createSupabaseClient(url, key) {
  defaultSerializer = KotlinXSerializer(Json {
    ignoreUnknownKeys = true
  })
  install(Postgrest)
}

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: LocalDateTime) {
    // Serialize LocalDateTime to a string (if needed)
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): LocalDateTime {
    // Deserialize the timestamp string into LocalDateTime
    val timestamp = decoder.decodeString()
    return kotlinx.datetime.Instant.parse(timestamp)
      .toLocalDateTime(TimeZone.UTC) // Convert to LocalDateTime in UTC
  }
}


fun main() {
  embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
    .start(wait = true)
}

fun Application.module() {
  install(GraphQL) {
    schema {
      packages = listOf("org.aryamahasangh")
      queries = listOf(
        OrgsQuery(),
        StudentAdmissionQuery()
      )
      mutations = listOf(
        OrgsMutation(),
        ActivityMutation(),
        StudentAdmissionMutations()
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

@Serializable
enum class ActivityPeriod {
  PAST,
  PRESENT,
  FUTURE,
  All
}

@Serializable
data class ActivityFilter(
  val type: ActivityType?,
  val state: String?,
  val district: String?,
  val activityPeriod: ActivityPeriod? = ActivityPeriod.All
)

@OptIn(ExperimentalUuidApi::class)
class OrgsQuery : Query {
  suspend fun organisations(): List<Organisation> = getOrganisations()
  suspend fun organisation(name: String): Organisation? = getOrganisations().find { it.name == name }
  suspend fun organisationalActivities(filter: ActivityFilter? = null): List<OrganisationalActivity> = getOrganisationalActivities(filter)
  suspend fun organisationalActivity(id: String) =  getOrganisationalActivity(id)
  suspend fun learningItems(): List<Video> = getVideos()
  suspend fun learningItem(id: String) = getVideos().find { it.id == id }
  suspend fun members(): List<Member> = getMembers().distinctBy { it.phoneNumber }
  suspend fun label(key: String): String = getLabel(key)
}

suspend fun getLabel(key: String): String {
  val label = supabase.from("labels").select(columns = Columns.ALL){
    filter {
      eq("key", key)
    }
  }.decodeSingle<Label>()
  println("label: $label")
  return label.label
}

@Serializable
data class OrganisationId(val organisation_id: String)

suspend fun getOrganisationalActivity(id: String): OrganisationalActivity? {
  return try {
    val activity = supabase.from("activities")
      .select(
        Columns.raw("*, contactPeople:activity_member(*, member:member(*))")
      ){
        filter {
          eq("id", id)
        }
      }.decodeSingleOrNull<OrganisationalActivity>()
    if(activity != null) {
      val orgIds = supabase.from("organisational_activity").select(Columns.list("organisation_id")){
        filter { eq("activity_id", id) }
      }.decodeList<OrganisationId>()
      val orgs = supabase.from("organisation").select(Columns.list("id", "name")){
        filter { isIn("id", orgIds.map { it.organisation_id }) }
      }.decodeList<Organisation>()
      activity.copy(associatedOrganisations = orgs)
    }else{
      null
    }
  }catch (e: Exception){
    println("Error: $e")
    null
  }
}

suspend fun getOrganisationalActivities(activityFilter: ActivityFilter?): List<OrganisationalActivity> {
  println("getOrganisationalActivities() called with: activityFilter = $activityFilter")
  //Columns.raw("*, contactPeople:activity_member(*, member:member(*))
  val newActivities = mutableListOf<OrganisationalActivity>()
  return try {
    val activities = supabase.from("activities")
      .select(
        Columns.raw("*, contactPeople:activity_member(*, member:member(*))")
      ){
        if(activityFilter != null) {
          filter {
            if(activityFilter.type != null) {
              eq("type", activityFilter.type)
            }
            if(!activityFilter.state.isNullOrEmpty()){
              eq("state", activityFilter.state)
            }
            if(!activityFilter.district.isNullOrEmpty()) {
              eq("district", activityFilter.district)
            }
            if(activityFilter.type != null && activityFilter.activityPeriod == ActivityPeriod.FUTURE) {
              val current = Clock.System.now().toLocalDateTime(TimeZone.Companion.of("Asia/Kolkata"))
              println("currentDateTime: $current")
              gt("start_datetime", current)
            }
          }
        }
      }.decodeList<OrganisationalActivity>()
    activities.forEach { it ->
      val orgIds = supabase.from("organisational_activity").select(Columns.list("organisation_id")){
        filter { eq("activity_id", it.id) }
      }.decodeList<OrganisationId>()
      val orgs = supabase.from("organisation").select(Columns.list("id", "name")){
        filter { isIn("id", orgIds.map { it.organisation_id }) }
      }.decodeList<Organisation>()
      newActivities.add(it.copy(associatedOrganisations = orgs))
    }
//    println("activities: $newActivities")
    newActivities
  }catch (e: Exception){
    println("Error: $e")
    listOf()
  }
}

suspend fun getOrganisations(): List<Organisation> {
  return supabase.from("organisation")
    .select(
      Columns.raw("*, keyPeople:organisational_member(*, member:member(*))")
    )
    .decodeList<Organisation>()
}

suspend fun getOrganisation(id: String): Organisation? {
  return supabase.from("organisation")
    .select(
      Columns.raw("*, keyPeople:organisational_member(*, member:member(*))")
    ){
      filter { Organisation::id eq id }
    }
    .decodeSingleOrNull<Organisation>()
}

suspend fun getVideos(): List<Video> {
  return supabase.from("learning").select().decodeList<Video>()
}

suspend fun getMembers(): List<Member> {
  // select distinct on (phone_number) * from member
  return supabase.from("member").select().decodeList<Member>()
}

@OptIn(ExperimentalUuidApi::class)
class StudentAdmissionMutations : Mutation {
  suspend fun addStudentAdmissionData(input: AdmissionFormData): Boolean{
    println("input student data: $input")
    try {
      supabase.from("admission").insert(input)
      return true
    } catch (e: Exception) {
      println("error: $e")
      return false
    }
  }
}

class StudentAdmissionQuery : Query {
  suspend fun studentsApplied(): List<AdmissionFormData> {
    val res = supabase.from("admission").select().decodeList<AdmissionFormData>()
    return res
  }
}

@Serializable
data class Label(
  val key: String,
  val label: String
)

@OptIn(ExperimentalUuidApi::class)
class OrgsMutation : Mutation {

  suspend fun updateJoinUsLabel(label: String): Boolean {
    try {
      supabase.from("labels").upsert(Label("join_us", label)){
        onConflict = "key"
        ignoreDuplicates = false
      }
      return true
    }catch (e: Exception){
      println(e)
      return false
    }
  }

//  fun addOrganisation(input: OrganisationInput): Boolean {
//    val org = Organisation(
//      id = Uuid.random().toString(),
//      name = input.name,
//      logo = input.logo,
//      description = input.description,
//      keyPeople = input.people
//    )
//    OrganisationPublisher.publishOrganisation(org)
//    return true
//  }
//  fun addMemberToOrganisation(orgId: Uuid, orgMember: OrganisationalMember): Boolean {
//    return false
//  }
//
//  fun removeMemberFromOrganisation(orgId: Uuid, memberId: Uuid): Boolean {
//    return false
//  }
//
//  fun updateOrganisationMember(orgId: Uuid, memberId: Uuid, orgMemberDetails: OrganisationalMember): Boolean {
//    return false
//  }
//
//  fun updateOrganisationDetails(orgId: Uuid, name: String, description: String, logo: String): Boolean {
//    return false
//  }
//
//  fun removeOrganisation(orgId: Uuid): Boolean {
//    return false
//  }
}

@Serializable
data class InsertResponse(
  val id: String
)

@OptIn(ExperimentalUuidApi::class)
class ActivityMutation : Mutation {
  suspend fun addOrganisationActivity(input: OrganisationActivityInput): Boolean {
    println("input: $input")
    try{
      val (id) = supabase.from("activities").insert(ActivityInput(
        name = input.name,
        shortDescription = input.shortDescription,
        longDescription = input.longDescription,
        activityType = input.activityType,
        startDateTime = input.startDateTime,
        endDateTime = input.endDateTime,
        address = input.address,
        state = input.state,
        district = input.district,
        mediaFiles = input.mediaFiles,
        additionalInstructions =  input.additionalInstructions
      )){
        select(Columns.raw("id"))
      }.decodeSingle<InsertResponse>()

      input.associatedOrganisations.forEach { orgId ->
        supabase.from("organisational_activity")
          .insert(
            Organisational_Activity(id, orgId)
          )
      }

      input.contactPeople.forEach { contactPerson ->
        supabase.from("activity_member")
          .insert(
            Activity_Member(
              activity_id = id,
              member_id = contactPerson[0],
              post = contactPerson[1],
              priority = contactPerson[2].toInt()
            )
          )
        }
      return true
    }catch (e: Exception){
      println(e)
      return false
    }
  }

  suspend fun removeActivity(activityId: String): Boolean {
    println("removeActivity: $activityId")
    try {
      supabase.from("activities").delete {
        filter {
          eq("id", activityId)
        }
      }
      return true
    }catch (e: Exception){
      println("error: $e")
      return false
    }
  }
  fun updateActivity(activity: OrganisationalActivity): Boolean {
    return false
  }
}


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
