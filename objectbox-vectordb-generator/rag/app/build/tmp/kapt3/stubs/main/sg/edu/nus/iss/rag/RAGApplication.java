package sg.edu.nus.iss.rag;

@kotlin.Metadata(mv = {2, 4, 0}, k = 1, xi = 48, d1 = {"\u0000r\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0014\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u0011\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u0007H\u0002J\u001a\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u00052\b\b\u0002\u0010\u001e\u001a\u00020\u001fH\u0002J,\u0010 \u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020#0\"0!2\u0006\u0010$\u001a\u00020\u00072\b\b\u0002\u0010%\u001a\u00020&H\u0002J0\u0010\'\u001a\u00020\u00072\u0006\u0010$\u001a\u00020\u00072\u0018\u0010(\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020#0\"0!H\u0082@\u00a2\u0006\u0002\u0010)J\u001f\u0010*\u001a\u00020\u001c2\f\u0010+\u001a\b\u0012\u0004\u0012\u00020\u00070,H\u0007b\u0002\b.\u00a2\u0006\u0002\u0010-R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\f\u001a\u00020\r8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0010\u0010\u0011\u001a\u0004\b\u000e\u0010\u000fR!\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u00138BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0017\u0010\u0011\u001a\u0004\b\u0015\u0010\u0016\u00a8\u0006/"}, d2 = {"Lsg/edu/nus/iss/rag/RAGApplication;", "", "<init>", "()V", "DATA_DIR", "Ljava/io/File;", "CHAT_MODEL", "", "embeddingModel", "Ldev/langchain4j/model/embedding/onnx/allminilml6v2/AllMiniLmL6V2EmbeddingModel;", "client", "Lio/ktor/client/HttpClient;", "boxStore", "Lio/objectbox/BoxStore;", "getBoxStore", "()Lio/objectbox/BoxStore;", "boxStore$delegate", "Lkotlin/Lazy;", "dishBox", "Lio/objectbox/Box;", "Lsg/edu/nus/iss/rag/Dish;", "getDishBox", "()Lio/objectbox/Box;", "dishBox$delegate", "embed", "", "text", "ingestToObjectBox", "", "dirPath", "reset", "", "retrieve", "", "Lkotlin/Pair;", "", "query", "topK", "", "answer", "topChunks", "(Ljava/lang/String;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "main", "args", "", "([Ljava/lang/String;)V", "Lkotlin/jvm/JvmStatic;", "rag:app"})
public final class RAGApplication {
    @org.jetbrains.annotations.NotNull()
    private static final java.io.File DATA_DIR = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CHAT_MODEL = "llama3.1";
    @org.jetbrains.annotations.NotNull()
    private static final dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel embeddingModel = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.ktor.client.HttpClient client = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.Lazy boxStore$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.Lazy dishBox$delegate = null;
    @org.jetbrains.annotations.NotNull()
    public static final sg.edu.nus.iss.rag.RAGApplication INSTANCE = null;
    
    private RAGApplication() {
        super();
    }
    
    private final io.objectbox.BoxStore getBoxStore() {
        return null;
    }
    
    private final io.objectbox.Box<sg.edu.nus.iss.rag.Dish> getDishBox() {
        return null;
    }
    
    private final float[] embed(java.lang.String text) {
        return null;
    }
    
    private final void ingestToObjectBox(java.io.File dirPath, boolean reset) {
    }
    
    private final java.util.List<kotlin.Pair<sg.edu.nus.iss.rag.Dish, java.lang.Double>> retrieve(java.lang.String query, int topK) {
        return null;
    }
    
    private final java.lang.Object answer(java.lang.String query, java.util.List<kotlin.Pair<sg.edu.nus.iss.rag.Dish, java.lang.Double>> topChunks, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @kotlin.jvm.JvmStatic()
    public static final void main(@org.jetbrains.annotations.NotNull()
    java.lang.String[] args) {
    }
}