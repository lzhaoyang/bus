/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 ********************************************************************************/
package org.aoju.bus.image.metric.internal.xdsi;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kimi Liu
 * @version 5.9.3
 * @since JDK 1.8+
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RetrieveDocumentSetRequestType", namespace = "urn:ihe:iti:xds-b:2007", propOrder = {"documentRequest"})
public class RetrieveDocumentSetRequestType {

    @XmlElement(name = "DocumentRequest", required = true)
    protected List<DocumentRequest> documentRequest;

    public List<DocumentRequest> getDocumentRequest() {
        if (this.documentRequest == null) {
            this.documentRequest = new ArrayList();
        }
        return this.documentRequest;
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"homeCommunityId", "repositoryUniqueId", "documentUniqueId"})
    public static class DocumentRequest {

        @XmlElement(name = "HomeCommunityId", namespace = "urn:ihe:iti:xds-b:2007")
        protected String homeCommunityId;

        @XmlElement(name = "RepositoryUniqueId", namespace = "urn:ihe:iti:xds-b:2007", required = true)
        protected String repositoryUniqueId;

        @XmlElement(name = "DocumentUniqueId", namespace = "urn:ihe:iti:xds-b:2007", required = true)
        protected String documentUniqueId;

        public String getHomeCommunityId() {
            return this.homeCommunityId;
        }

        public void setHomeCommunityId(String value) {
            this.homeCommunityId = value;
        }

        public String getRepositoryUniqueId() {
            return this.repositoryUniqueId;
        }

        public void setRepositoryUniqueId(String value) {
            this.repositoryUniqueId = value;
        }

        public String getDocumentUniqueId() {
            return this.documentUniqueId;
        }

        public void setDocumentUniqueId(String value) {
            this.documentUniqueId = value;
        }

    }

}
