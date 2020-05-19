class Message() {
    var text: String? = null
    var attachments: ArrayList<Attachment> = arrayListOf()
    constructor(text: String) : this() {
        this.text = text
    }

    fun addAttachment(attachment: Attachment) {
        this.attachments.add(attachment)
    }
    fun hasAttachment() : Boolean {
        return !this.attachments.isEmpty()
    }
}

class Attachment {
    companion object {
        fun fromIssue(issue: Issue) : Attachment {
            val a = Attachment()
            a.title = issue.title
            a.title_link = issue.webUrl
            a.footer = issue.labels.flat()
            a.text = "<@${issue.assignee?.username ?: issue.author?.username}>"
            return a
        }
    }
    var author_icon: String? = null
    var author_link: String? = null
    var author_name: String? = null
    var color: String? = null
    var fallback: String? = null
//    var fields: ArrayList<Field> = arrayListOf()
    var footer: String? = null
    var footer_icon: String? = null
    var image_url: String? = null
    var pretext: String? = null
    var text: String? = null
    var thumb_url: String? = null
    var title: String? = null
    var title_link: String? = null
    var ts: Int? = null
}

class Field {
    var short: Boolean? = null
    var title: String? = null
    var value: String? = null
}