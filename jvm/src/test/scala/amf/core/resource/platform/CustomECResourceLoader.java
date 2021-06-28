package amf.core.resource.platform;

import amf.core.client.common.remote.Content;
import amf.core.client.platform.resource.LoaderWithExecutionContext;
import amf.core.client.platform.resource.ResourceLoader;
import scala.concurrent.ExecutionContext;

import java.util.concurrent.CompletableFuture;

/**
 * This test is a use case that can be used when creating resource loaders that internally use AMF provided resource loaders
 * which receive an execution context, such as FileResourceLoader or HttpResourceLoader
 */
public class CustomECResourceLoader implements ResourceLoader, LoaderWithExecutionContext {

    private final ExecutionContext ec;

    public CustomECResourceLoader(ExecutionContext ec) {
        this.ec = ec;
    }

    public ExecutionContext getEc() {
        return ec;
    }

    @Override
    public ResourceLoader withExecutionContext(ExecutionContext ec) {
        return new CustomECResourceLoader(ec);
    }

    @Override
    public CompletableFuture<Content> fetch(String resource) {
        return null;
    }

    @Override
    public boolean accepts(String resource) {
        return true;
    }
}
