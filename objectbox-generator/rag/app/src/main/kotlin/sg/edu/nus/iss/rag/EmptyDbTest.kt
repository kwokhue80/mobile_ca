package sg.edu.nus.iss.rag

fun main() {
    val emptyStore = MyObjectBox.builder()
        .name("chat-size-test-empty")
        .build()
    emptyStore.close()
    println("Empty database created. Check its file size now.")
}