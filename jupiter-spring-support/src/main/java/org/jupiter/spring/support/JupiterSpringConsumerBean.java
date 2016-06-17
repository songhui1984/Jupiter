/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.spring.support;

import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;

/**
 * Consumer bean, 负责构造并初始化 consumer 代理对象.
 *
 * jupiter
 * org.jupiter.spring.support
 *
 * @author jiachun.fjc
 */
public class JupiterSpringConsumerBean<T> implements FactoryBean<T>, InitializingBean {

    private static final ConsumerHook[] EMPTY_HOOKS = new ConsumerHook[0];

    private JupiterSpringConnector connector;
    private Class<T> interfaceClass;                        // 接口类型
    private long waitForAvailableTimeoutMillis = -1;        // 默认建立连接的超时时间 <=0 表示不等待连接建立成功

    private transient T proxy;                              // consumer代理对象

    private InvokeType invokeType;                          // 调用方式 [同步; 异步promise; 异步callback]
    private DispatchType dispatchType;                      // 派发方式 [单播; 组播]
    private long timeoutMillis;                             // 调用超时时间设置
    private Map<String, Long> methodsSpecialTimeoutMillis;  // 指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private JListener listener;                             // 回调函数
    private ConsumerHook[] hooks = EMPTY_HOOKS;             // consumer hook

    @Override
    public T getObject() throws Exception {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() {
        ProxyFactory<T> factory = ProxyFactory.factory(interfaceClass);
        JConnector<JConnection> client = connector.getConnector();

        if (connector.isHasRegistryServer()) {
            // 自动管理可用连接
            JConnector.ConnectionManager manager = client.manageConnections(interfaceClass);
            if (waitForAvailableTimeoutMillis > 0) {
                // 等待连接可用
                if (!manager.waitForAvailable(waitForAvailableTimeoutMillis)) {
                    throw new ConnectFailedException();
                }
            }
        } else {
            List<UnresolvedAddress> addresses = connector.getProviderServerUnresolvedAddresses();
            for (UnresolvedAddress address : addresses) {
                client.connect(address, true);  // 异步
            }
            factory.addProviderAddress(addresses);
        }

        if (invokeType != null) {
            factory.invokeType(invokeType);
        }

        if (dispatchType != null) {
            factory.dispatchType(dispatchType);
        }

        if (timeoutMillis > 0) {
            factory.timeoutMillis(timeoutMillis);
        }

        if (methodsSpecialTimeoutMillis != null) {
            for (Map.Entry<String, Long> entry : methodsSpecialTimeoutMillis.entrySet()) {
                factory.methodSpecialTimeoutMillis(entry.getKey(), entry.getValue());
            }
        }

        if (listener != null) {
            factory.listener(listener);
        }

        if (hooks.length > 0) {
            factory.addHook(hooks);
        }

        proxy = factory
                .connector(client)  // Sets the connector
                .newProxyInstance();
    }

    public JupiterSpringConnector getConnector() {
        return connector;
    }

    public void setConnector(JupiterSpringConnector connector) {
        this.connector = connector;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public long getWaitForAvailableTimeoutMillis() {
        return waitForAvailableTimeoutMillis;
    }

    public void setWaitForAvailableTimeoutMillis(long waitForAvailableTimeoutMillis) {
        this.waitForAvailableTimeoutMillis = waitForAvailableTimeoutMillis;
    }

    public InvokeType getInvokeType() {
        return invokeType;
    }

    public void setInvokeType(InvokeType invokeType) {
        this.invokeType = invokeType;
    }

    public DispatchType getDispatchType() {
        return dispatchType;
    }

    public void setDispatchType(DispatchType dispatchType) {
        this.dispatchType = dispatchType;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public Map<String, Long> getMethodsSpecialTimeoutMillis() {
        return methodsSpecialTimeoutMillis;
    }

    public void setMethodsSpecialTimeoutMillis(Map<String, Long> methodsSpecialTimeoutMillis) {
        this.methodsSpecialTimeoutMillis = methodsSpecialTimeoutMillis;
    }

    public JListener getListener() {
        return listener;
    }

    public void setListener(JListener listener) {
        this.listener = listener;
    }

    public ConsumerHook[] getHooks() {
        return hooks;
    }

    public void setHooks(ConsumerHook[] hooks) {
        this.hooks = hooks;
    }
}
