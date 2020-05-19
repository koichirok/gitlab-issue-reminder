import com.squareup.moshi.Json
import java.util.Date

val DAY_MILLIS = 1000 * 60 * 60 * 24

data class Issue(
    val title: String,
    val state: String,
    @field:Json(name = "labels") val labels: List<String>,
    @field:Json(name = "web_url") val webUrl: String,
    @field:Json(name = "due_date") val dueDate: Date?,
    val author: User?,
    val assignee: User?
) {
    fun daysUntilDue(fromTime: Long) : Int {
        if (this.dueDate == null) {
            return Int.MAX_VALUE
        }
        return ((this.dueDate.time - fromTime) / DAY_MILLIS).toInt()
    }

    fun isOverdue(time: Long) : Boolean {
        return (this.dueDate?.time ?: Long.MAX_VALUE) < time
    }
}

data class User(
    val avatar_url: Any,
    val id: Int,
    val name: String,
    val state: String,
    val username: String,
    val web_url: String
)