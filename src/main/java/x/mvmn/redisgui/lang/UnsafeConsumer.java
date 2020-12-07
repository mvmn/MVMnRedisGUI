package x.mvmn.redisgui.lang;

public interface UnsafeConsumer<T> {

	void accept(T t) throws Exception;

}
