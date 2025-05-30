package  com.mana.glym.orm.testEntity

import com.mana.glym.annotation.Entity

@Entity
class User {
    var id: Int = 0
    var name: String = ""
    var email: String = ""


    override fun toString(): String {
        return "User(id='$id', name='$name', email='$email')"
    }

    fun foo() {
        println("foo")
    }
}