import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.Date

val moshi = Moshi.Builder()
        .add(Date::class.java, CustomDateFormatAdapter())
        .build()
val listType = Types.newParameterizedType(List::class.java, Issue::class.java)
val adapter: JsonAdapter<List<Issue>> = moshi.adapter(listType)
val property: Property by lazy { initializeProperty() }
val FETCH_COUNT = 100

fun initializeProperty(): Property {
    return Property(System.getenv(Env.HOST),
            System.getenv(Env.GITLAB_TOKEN),
            System.getenv(Env.SLACK_WEB_HOOK_URL),
            System.getenv(Env.PROJECT_ID).toInt(),
            System.getenv(Env.LIMIT).toInt())
}

fun main(args: Array<String>) {
    assertionSystemEnv()
    println("Env OK! Start fetch from ${property.host}")
    FuelManager.instance.basePath = property.host
    // TODO: Coroutineに置き換え
    fetch {
        println("All issue size: ${it.size}")
        val msgs = buildMessages(it)
        postToSlack(msgs)
    }
}

fun assertionSystemEnv() {
    requireNotNull(System.getenv(Env.HOST)) { "GitLabのホストを ${Env.HOST} に設定してください。" }
    requireNotNull(System.getenv(Env.GITLAB_TOKEN)) { "GitLabのトークンを ${Env.GITLAB_TOKEN} に設定してください。" }
    requireNotNull(System.getenv(Env.SLACK_WEB_HOOK_URL)) { "SlackのWebHook URLを ${Env.SLACK_WEB_HOOK_URL} に設定してください。" }
    requireNotNull(System.getenv(Env.PROJECT_ID)) { "プロジェクトのIDを ${Env.PROJECT_ID} に設定してください。" }
    requireNotNull(System.getenv(Env.LIMIT)) { "リマインドを促し始める日数を ${Env.LIMIT} に設定してください。" }
    require(System.getenv(Env.PROJECT_ID).toIntOrNull() != null) { "${Env.PROJECT_ID} には数字を設定してください。" }
    require(System.getenv(Env.LIMIT).toIntOrNull() != null) { "${Env.LIMIT} には数字を設定してください。" }
}

fun fetch(issueList: ArrayList<Issue> = arrayListOf(), page: Int = 1, callback: (ArrayList<Issue>) -> Unit) {
    val (_, _, result) = "/api/v4/projects/${property.projectId}/issues"
            .httpGet(listOf("state" to "opened", "page" to page, "per_page" to FETCH_COUNT))
            .header("PRIVATE-TOKEN" to property.gitlabToken)
            .responseString()
    result.fold({ json ->
        val issues = adapter.fromJson(json)?: run {
            println("Failed deserialized Issue from JSON.")
            return System.exit(1)
        }
        issueList.addAll(issues)
        when (issues.size < FETCH_COUNT) {
            true -> callback(issueList)
            false -> fetch(issueList, page + 1, callback)
        }
    }, {
        println(it.localizedMessage)
        return System.exit(1)
    })
}

fun buildMessages(issueList: ArrayList<Issue>): List<Message> {
    val time = Date().time
    val overdueMsg = Message("締切を過ぎてるIssue")
    val upcomingMsg = Message("締切が${property.limit}日以内のIssue")
    issueList
            .filter { it.daysUntilDue(time) < property.limit } // 締切が遠いもの(と設定されていないもの)は除外
            .sortedByDescending { it.dueDate } // 時系列にソート
            .forEach {
                val attachment = Attachment.fromIssue(it)
                if (it.isOverdue(time)) {
                    attachment.color = "danger"
                    overdueMsg.addAttachment(attachment)
                } else {
                    attachment.color = "warning"
                    upcomingMsg.addAttachment(attachment)
                }
            }
    return listOf(overdueMsg, upcomingMsg)
}

fun postToSlack(messages: List<Message>) {
    val moshi = Moshi.Builder()
            .add(MessageJsonAdapter.FACTORY)
            .add(ListAttachmentJsonAdapter.FACTORY)
            .add(AttachmentJsonAdapter.FACTORY)
            .build()

    val listType = Types.newParameterizedType(Message::class.java)
    val adapter: JsonAdapter<Message> = moshi.adapter(listType)

    // TODO: 0件だった場合ハッピーな文言を送る
    // TODO: エラーハンドリング
    messages.forEach {
            if (it.hasAttachment())
                Fuel.post(property.slackWebHookUrl)
                        .header(mapOf("Content-Type" to "application/json"))
                        .body(adapter.toJson(it)).response()
    }
}

fun List<String>.flat(): String {
    return this
            .ifEmpty { listOf("") }
            .reduce { acc, s -> "$acc, $s" }
}