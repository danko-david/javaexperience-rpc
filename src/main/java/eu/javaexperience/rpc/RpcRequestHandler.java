package eu.javaexperience.rpc;

import eu.javaexperience.dispatch.Dispatcher;

public interface RpcRequestHandler<C extends RpcRequest> extends Dispatcher<C>{}