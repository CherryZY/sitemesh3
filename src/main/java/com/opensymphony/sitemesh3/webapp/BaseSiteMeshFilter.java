package com.opensymphony.sitemesh3.webapp;

import com.opensymphony.sitemesh3.content.ContentProcessor;
import com.opensymphony.sitemesh3.DecoratorSelector;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.webapp.contentfilter.ContentBufferingFilter;
import com.opensymphony.sitemesh3.webapp.contentfilter.Selector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.CharBuffer;

/**
 * Core Filter for integrating SiteMesh into a Java web application.
 *
 * <p>For this to be functional it requires a {@link Selector}, and {@link ContentProcessor}.
 * These must be passed in through the constructor or setter methods.</p>
 *
 * <p>This filter will not work on its own in a typical Servlet container as the container
 * will not know how to pass in the dependencies. It is designed for programmatic use, or
 * to work with frameworks that can inject dependencies. Alternatively, it can be
 * subclassed.</p>
 *
 * @author Joe Walnes
 * @author Scott Farquhar
 */
public class BaseSiteMeshFilter extends ContentBufferingFilter {

    private Selector selector;
    private ContentProcessor contentProcessor;
    private DecoratorSelector<WebAppContext> decoratorSelector;

    /**
     * Default constructor. If this is used, it is the caller's
     * responsibility to call {@link #setSelector(Selector)},
     * {@link #setContentProcessor(ContentProcessor)},
     * and {@link #setDecoratorSelector(DecoratorSelector)}.
     */
    public BaseSiteMeshFilter() {
    }

    /**
     * Will call {@link #setSelector(Selector)},
     * {@link #setContentProcessor(ContentProcessor)},
     * and {@link #setDecoratorSelector(DecoratorSelector)}.
     */
    public BaseSiteMeshFilter(Selector selector,
                              ContentProcessor contentProcessor,
                              DecoratorSelector<WebAppContext> decoratorSelector) {
        setSelector(selector);
        setContentProcessor(contentProcessor);
        setDecoratorSelector(decoratorSelector);
    }

    /**
     * The {@link Selector} provides the rules for whether SiteMesh should be
     * used for a specific request. For a basic implementation, use
     * {@link com.opensymphony.sitemesh3.webapp.contentfilter.BasicSelector}.
     */
    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public void setContentProcessor(ContentProcessor contentProcessor) {
        this.contentProcessor = contentProcessor;
    }

    public void setDecoratorSelector(DecoratorSelector<WebAppContext> decoratorSelector) {
        this.decoratorSelector = decoratorSelector;
    }

    @Override
    protected Selector getSelector(HttpServletRequest request) {
        return selector;
    }

    /**
     * @return Whether the content was processed. If false, the original content shall
     *         be written back out.
     */
    @Override
    protected boolean postProcess(String contentType, CharBuffer buffer,
                                  HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        verify();
        WebAppContext context = createContext(contentType, request, response);
        ContentProperty contentProperty = contentProcessor.build(buffer, context);
        if (contentProperty == null) {
            return false;
        }

        String[] decoratorPaths = decoratorSelector.selectDecoratorPaths(contentProperty, context);
        for (String decoratorPath : decoratorPaths) {
            contentProperty = context.decorate(decoratorPath, contentProperty);
        }

        if (contentProperty == null) {
            return false;
        }
        contentProperty.writeValueTo(response.getWriter());
        return true;
    }

    /**
     * Verify all dependencies are present.
     */
    protected void verify() throws ServletException {
        if (selector == null) {
            throw new ServletException(getClass().getName()
                    + " not initialized correctly. setSelector() not called");
        }
        if (contentProcessor == null) {
            throw new ServletException(getClass().getName()
                    + " not initialized correctly. setContentProcessor() not called");
        }
        if (decoratorSelector == null) {
            throw new ServletException(getClass().getName()
                    + " not initialized correctly. setDecoratorSelector() not called");
        }
    }

    /**
     * Create a context for the current request. This method can be overriden to allow for other
     * types of context.
     */
    protected WebAppContext createContext(String contentType, HttpServletRequest request,
                                          HttpServletResponse response) {
        return new WebAppContext(contentType, request, response,
                getFilterConfig().getServletContext(), contentProcessor);
    }
}