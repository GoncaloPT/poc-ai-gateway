package pt.goncalo.aigateway.edge;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Response wrapper with mutable headers.
 * <p>
 * Spring AI's MCP transport returns responses with read-only headers, while gateway filters (e.g. Bucket4j
 * {@code rateLimit}, which adds {@code X-RateLimit-Remaining}) add headers after the handler ran. Headers added to the
 * wrapper are written in addition to the original ones.
 */
final class MutableHeadersResponse implements ServerResponse {

	private final ServerResponse delegate;

	private final HttpHeaders headers = new HttpHeaders();

	private MutableHeadersResponse(ServerResponse delegate) {
		this.delegate = delegate;
		this.headers.putAll(delegate.headers());
	}

	/**
	 * Innermost route filter that makes the handler's response headers mutable for the filters around it.
	 *
	 * @return filter function
	 */
	static HandlerFilterFunction<ServerResponse, ServerResponse> filter() {
		return (request, next) -> new MutableHeadersResponse(next.handle(request));
	}

	@Override
	public HttpStatusCode statusCode() {
		return delegate.statusCode();
	}

	@Override
	public HttpHeaders headers() {
		return headers;
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		return delegate.cookies();
	}

	@Override
	public ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context)
			throws ServletException, IOException {
		headers.forEach((name, values) -> {
			List<String> original = delegate.headers().getOrEmpty(name);
			values.stream().filter(value -> !original.contains(value)).forEach(value -> response.addHeader(name, value));
		});
		return delegate.writeTo(request, response, context);
	}

}
