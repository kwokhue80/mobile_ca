package sg.edu.nus.iss.rag

// Author: Soo Kwok Heng assisted by Claude
fun main() {
    val emptyStore = MyObjectBox.builder()
        .name("chat-size-test-empty")
        .build()
    emptyStore.close()
    println("Empty database created. Check its file size now.")
}