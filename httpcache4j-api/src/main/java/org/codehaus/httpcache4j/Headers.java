/*
 * Copyright (c) 2008, The Codehaus. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.codehaus.httpcache4j;

import net.hamnaberg.funclite.*;

import org.codehaus.httpcache4j.mutable.MutableHeaders;
import org.codehaus.httpcache4j.preference.Charset;
import org.codehaus.httpcache4j.preference.Preference;
import org.codehaus.httpcache4j.util.CaseInsensitiveKey;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.*;


/**
 * A collection of headers.
 * All methods that modify the headers return a new Headers object. 
 *
 * @author <a href="mailto:hamnis@codehaus.org">Erlend Hamnaberg</a>
 */
public final class Headers implements Iterable<Header> {
    private final HeaderHashMap headers = new HeaderHashMap();

    public Headers() {
    }

    public Headers(final Headers headers) {
        this(headers.copyMap());
    }

    private Headers(final HeaderHashMap headers) {
        Preconditions.checkNotNull(headers, "The header map may not be null");
        this.headers.putAll(headers);
    }

    public List<Header> getHeaders(String name) {
        return headers.getAsHeaders(name);
    }

    public List<Directives> getDirectives(String name) {
        return CollectionOps.map(getHeaders(name), new Function<Header, Directives>() {
            @Override
            public Directives apply(Header input) {
                return input.getDirectives();
            }
        });
    }

    public Header getFirstHeader(String headerKey) {
        List<Header> headerList = getHeaders(headerKey);
        if (!headerList.isEmpty()) {
            return headerList.get(0);
        }
        return null;
    }

    public String getFirstHeaderValue(String headerKey) {
        Header header = getFirstHeader(headerKey);
        if (header != null) {
            return header.getValue();
        }
        return null;
    }

    public Directives getFirstHeaderValueAsDirectives(String headerKey) {
        Header header = getFirstHeader(headerKey);
        if (header != null) {
            return header.getDirectives();
        }
        return null;
    }

    public Headers add(Header header) {
        HeaderHashMap headers = copyMap();
        List<String> list = new ArrayList<String>(headers.get(header.getName()));
        if (!list.contains(header.getValue())) {
            list.add(header.getValue());
        }
        headers.put(header.getName(), list);
        return new Headers(headers);
    }

    public Headers add(String key, String value) {
        return add(new Header(key, value));
    }

    public Headers add(Iterable<Header> headers) {
        HeaderHashMap map = copyMap();
        for (Header header : headers) {
            List<String> list = new ArrayList<String>(map.get(header.getName()));
            if (!list.contains(header.getValue())) {
                list.add(header.getValue());
            }
            map.put(header.getName(), list);
        }
        return new Headers(map);
    }

    public Headers add(String name, Iterable<String> values) {
        HeaderHashMap heads = copyMap();
        List<String> list = new ArrayList<String>(headers.get(name));
        CollectionOps.addAll(list, values);
        heads.put(name, list);
        return new Headers(heads);
    }

    public Headers set(Header header) {
        HeaderHashMap headers = copyMap();
        headers.put(header.getName(), CollectionOps.of(header.getValue()));
        return new Headers(headers);
    }

    public Headers set(String name, String value) {
        return set(new Header(name, value));
    }

    public Headers set(Iterable<Header> headers) {
        HeaderHashMap map = copyMap();
        Headers copy = new Headers().add(headers);
        Set<String> keys = copy.keySet();
        for (String key : keys) {
            map.put(key, copy.headers.get(key));
        }
        return new Headers(map);
    }

    public boolean contains(Header header) {
        List<Header> values = getHeaders(header.getName());
        return values.contains(header);
    }

    public boolean contains(String headerName) {
        return !headers.get(headerName).isEmpty();
    }

    /**
     * @deprecated use {@link #contains(String)} instead
     */
    @Deprecated
    public boolean hasHeader(String headerName) {
        return !headers.get(headerName).isEmpty();
    }

    public Headers remove(String name) {
        HeaderHashMap heads = copyMap();
        heads.remove(name);
        return new Headers(heads);
    }

    public Iterator<Header> iterator() {
        return headers.headerIterator();
    }

    public Set<String> keySet() {
        return headers.keys();
    }

    public int size() {
        return headers.size();
    }

    public boolean isEmpty() {
        return headers.isEmpty();
    }

    public Headers asCacheable() {
        return HeaderUtils.cleanForCaching(this);
    }

    public boolean isCachable() {
        return HeaderUtils.hasCacheableHeaders(this);
    }

    public List<Preference<Locale>> getAcceptLanguage() {
        return Preference.parse(getFirstHeader(HeaderConstants.ACCEPT_LANGUAGE), Preference.LocaleParse);
    }

    public Headers withAcceptLanguage(List<Preference<Locale>> acceptLanguage) {
        return set(Preference.toHeader(HeaderConstants.ACCEPT_LANGUAGE, acceptLanguage, Preference.LocaleToString));
    }

    public List<Preference<Charset>> getAcceptCharset() {
        return Preference.parse(getFirstHeader(HeaderConstants.ACCEPT_CHARSET), Preference.CharsetParse);
    }

    public Headers withAcceptCharset(List<Preference<Charset>> charsets) {
        return set(Preference.toHeader(HeaderConstants.ACCEPT_CHARSET, charsets, Preference.CharsetToString));
    }

    public List<Preference<MIMEType>> getAccept() {
        return Preference.parse(getFirstHeader(HeaderConstants.ACCEPT), Preference.MIMEParse);
    }

    public Headers withAccept(List<Preference<MIMEType>> charsets) {
        return set(Preference.toHeader(HeaderConstants.ACCEPT, charsets, Preference.<MIMEType>toStringF()));
    }

    public Headers addAccept(Preference<MIMEType>... accept) {
        List<Preference<MIMEType>> preferences = Arrays.asList(accept);
        return add(Preference.toHeader(HeaderConstants.ACCEPT, preferences, Preference.<MIMEType>toStringF()));
    }

    public Headers addAccept(MIMEType... accept) {
        List<Preference<MIMEType>> preferences = Preference.wrap(accept);
        return add(Preference.toHeader(HeaderConstants.ACCEPT, preferences, Preference.<MIMEType>toStringF()));
    }

    public Headers addAcceptLanguage(Locale... accept) {
        List<Preference<Locale>> preferences = Preference.wrap(accept);
        return add(Preference.toHeader(HeaderConstants.ACCEPT_LANGUAGE, preferences, Preference.LocaleToString));
    }

    public Headers addAcceptLanguage(Preference<Locale>... accept) {
        List<Preference<Locale>> preferences = Arrays.asList(accept);
        return add(Preference.toHeader(HeaderConstants.ACCEPT_LANGUAGE, preferences, Preference.LocaleToString));
    }

    public Headers addAcceptCharset(Charset... accept) {
        List<Preference<Charset>> preferences = Preference.wrap(accept);
        return add(Preference.toHeader(HeaderConstants.ACCEPT_LANGUAGE, preferences, Preference.CharsetToString));
    }

    public Headers addAcceptCharset(Preference<Charset>... accept) {
        List<Preference<Charset>> preferences = Arrays.asList(accept);
        return add(Preference.toHeader(HeaderConstants.ACCEPT_LANGUAGE, preferences, Preference.CharsetToString));
    }

    public Set<HTTPMethod> getAllow() {
        Header header = getFirstHeader(HeaderConstants.ALLOW);
        if (header != null) {
            Set<HTTPMethod> builder = CollectionOps.newLinkedHashSet();
            for (Directive directive : header.getDirectives()) {
                builder.add(HTTPMethod.valueOf(directive.getName().toUpperCase(Locale.ENGLISH)));
            }
            return builder;
        }
        return Collections.emptySet();
    }

    public Headers withAllow(Set<HTTPMethod> allow) {
        if (!allow.isEmpty()) {
            String allowValue = CollectionOps.mkString(allow, ",");
            return set(HeaderConstants.ALLOW, allowValue);
        }
        return remove(HeaderConstants.ALLOW);
    }

    public CacheControl getCacheControl() {
        return new CacheControl(getFirstHeader(HeaderConstants.CACHE_CONTROL));
    }

    public Headers withCacheControl(CacheControl cc) {
        return set(cc.toHeader());
    }

    public DateTime getDate() {
        return HeaderUtils.fromHttpDate(getFirstHeader(HeaderConstants.DATE));
    }

    public Headers withDate(DateTime dt) {
        return set(HeaderUtils.toHttpDate(HeaderConstants.DATE, dt));
    }

    public MIMEType getContentType() {
        String ct = getFirstHeaderValue(HeaderConstants.CONTENT_TYPE);
        return ct == null ? null : MIMEType.valueOf(ct);
    }

    public Headers withContentType(MIMEType ct) {
        return set(HeaderConstants.CONTENT_TYPE, ct.toString());
    }

    public DateTime getExpires() {
        return HeaderUtils.fromHttpDate(getFirstHeader(HeaderConstants.EXPIRES));
    }

    public Headers withExpires(DateTime expires) {
        return set(HeaderUtils.toHttpDate(HeaderConstants.EXPIRES, expires));
    }

    public DateTime getLastModified() {
        return HeaderUtils.fromHttpDate(getFirstHeader(HeaderConstants.LAST_MODIFIED));
    }

    public Headers withLastModified(DateTime lm) {
        return set(HeaderUtils.toHttpDate(HeaderConstants.LAST_MODIFIED, lm));
    }

    public Conditionals getConditionals() {
        return Conditionals.valueOf(this);
    }

    public Headers withConditionals(Conditionals conditionals) {
        return add(conditionals.toHeaders());
    }

    public Tag getETag() {
        Header tag = getFirstHeader(HeaderConstants.ETAG);
        if (tag != null) {
            return Tag.parse(tag.getValue());
        }
        return null;
    }

    public Headers withETag(Tag tag) {
        return set(HeaderConstants.ETAG, tag.format());
    }

    public URI getLocation() {
        String location = getFirstHeaderValue(HeaderConstants.LOCATION);
        if (location != null) {
            return URI.create(location);
        }
        return null;
    }

    public Headers withLocation(URI href) {
        return set(HeaderConstants.LOCATION, href.toString());
    }

    public URI getContentLocation() {
        String location = getFirstHeaderValue(HeaderConstants.CONTENT_LOCATION);
        if (location != null) {
            return URI.create(location);
        }
        return null;
    }

    public Headers withContentLocation(URI href) {
        return set(HeaderConstants.CONTENT_LOCATION, href.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Headers headers1 = (Headers) o;

        return headers.equals(headers1.headers);
    }

    @Override
    public int hashCode() {
        return headers.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Header header : this) {
            if (builder.length() > 0) {
                builder.append("\r\n");
            }
            builder.append(header);
        }
        return builder.toString();
    }

    private HeaderHashMap copyMap() {
        return new HeaderHashMap(headers);
    }

    public static Headers parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new Headers();
        }
        MutableHeaders headers = new MutableHeaders();
        String[] fields = input.split("\r\n");
        for (String field : fields) {
            headers.add(Header.valueOf(field.trim()));
        }
        return headers.toHeaders();
    }


    private static class HeaderHashMap extends LinkedHashMap<CaseInsensitiveKey, List<String>> {
        private static final long serialVersionUID = 2714358409043444835L;

        private static final Function<Header,String> headerToString = new Function<Header, String>() {
            public String apply(Header from) {
                return from.getValue();
            }
        };

        public HeaderHashMap() {
        }

        public HeaderHashMap(HeaderHashMap headerHashMap) {
            super(headerHashMap);
        }

        public List<String> get(String key) {
            return get(new CaseInsensitiveKey(key));
        }

        public Set<String> keys() {
            Set<String> strings = new HashSet<String>();
            for (CaseInsensitiveKey name : super.keySet()) {
                strings.add(name.getDelegate());
            }
            return strings;
        }

        @Override
        public List<String> get(Object key) {
            List<String> value = super.get(key);
            return value != null ? value : Collections.<String>emptyList();
        }

        List<Header> getAsHeaders(final String key) {
            List<Header> headers = new ArrayList<Header>();
            CaseInsensitiveKey name = new CaseInsensitiveKey(key);
            headers.addAll(CollectionOps.map(get(name), nameToHeader(name)));
            return Collections.unmodifiableList(headers);
        }

        private Function<String, Header> nameToHeader(final CaseInsensitiveKey key) {
            return new Function<String, Header>() {
                    public Header apply(String from) {
                        return new Header(key.getDelegate(), from);
                    }
                };
        }

        public List<String> put(String key, List<String> value) {
            return super.put(new CaseInsensitiveKey(key), value);
        }

        public List<String> remove(String key) {
            return remove(new CaseInsensitiveKey(key));
        }

        Iterator<Header> headerIterator() {
            List<Header> headers = new ArrayList<Header>();
            for (Map.Entry<CaseInsensitiveKey, List<String>> entry : this.entrySet()) {
                headers.addAll(CollectionOps.map(entry.getValue(), nameToHeader(entry.getKey())));
            }
            return headers.iterator();
        }
    }
}
