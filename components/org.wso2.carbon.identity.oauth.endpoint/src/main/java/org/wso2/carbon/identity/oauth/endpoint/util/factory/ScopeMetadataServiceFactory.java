/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.endpoint.util.factory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.OAuth2ScopeService;
import org.wso2.carbon.identity.oauth2.scopeservice.APIResourceBasedScopeMetadataService;
import org.wso2.carbon.identity.oauth2.scopeservice.ScopeMetadataService;

import java.util.List;

/**
 * Factory class for ScopeMetadataService.
 */
public class ScopeMetadataServiceFactory {

    private static final ScopeMetadataService SERVICE;

    private static final Log LOG = LogFactory.getLog(ScopeMetadataServiceFactory.class);

    static {
        ScopeMetadataService scopeMetadataService = setScopeMetadataService();

        if (scopeMetadataService == null) {
            throw new IllegalStateException("ScopeMetadataService is not available from OSGi context.");
        }
        SERVICE = scopeMetadataService;
    }

    private static ScopeMetadataService setScopeMetadataService() {

        ScopeMetadataService scopeMetadataService = getScopeMetadataServiceFromConfig();
        if (scopeMetadataService != null) {
            return scopeMetadataService;
        }

        // Get the OSGi services registered for ScopeService interface.
        List<Object> scopeServices = PrivilegedCarbonContext
                .getThreadLocalCarbonContext().getOSGiServices(ScopeMetadataService.class, null);
        if (scopeServices != null && !scopeServices.isEmpty()) {
            if (CarbonConstants.ENABLE_LEGACY_AUTHZ_RUNTIME) {
                for (Object scopeService : scopeServices) {
                    if (scopeService instanceof OAuth2ScopeService) {
                        scopeMetadataService = (ScopeMetadataService) scopeService;
                    }
                }
            } else {
                for (Object scopeService : scopeServices) {
                    if (scopeService instanceof APIResourceBasedScopeMetadataService) {
                        scopeMetadataService = (APIResourceBasedScopeMetadataService) scopeService;
                    }
                }
            }
        }

        if (scopeMetadataService == null) {
            throw new IllegalStateException("ScopeMetadataService is not available from OSGi context.");
        }
        return scopeMetadataService;
    }

    private static ScopeMetadataService getScopeMetadataServiceFromConfig() {

        String scopeMetadataServiceClassName = OAuthServerConfiguration.getInstance()
                .getScopeMetadataExtensionImpl();
        if (scopeMetadataServiceClassName != null) {
            try {
                String className = StringUtils.trimToEmpty(scopeMetadataServiceClassName);
                Class<?> clazz = Class.forName(className);
                Object obj = clazz.newInstance();
                if (obj instanceof ScopeMetadataService) {
                    return (ScopeMetadataService) obj;
                } else {
                    LOG.error(scopeMetadataServiceClassName + " is not an instance of " +
                            ScopeMetadataService.class.getName());
                }
            } catch (ClassNotFoundException e) {
                LOG.error("ClassNotFoundException while trying to find class " + scopeMetadataServiceClassName);
            } catch (InstantiationException e) {
                LOG.error("InstantiationException while trying to instantiate class " +
                        scopeMetadataServiceClassName);
            } catch (IllegalAccessException e) {
                LOG.error("IllegalAccessException while trying to instantiate class " +
                        scopeMetadataServiceClassName);
            }
        }
        return null;
    }

    public static ScopeMetadataService getScopeMetadataService() {

        return SERVICE;
    }
}
