package ch.ltouroumov.heig.amt.project1.router;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by ldavid on 9/28/16.
 */
public class RouterFilter implements Filter {

    private Router router;
    private final static Logger LOG = Logger.getLogger("RouterFilter");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String configName = filterConfig.getInitParameter("configurator");

        router = new Router();

        try {
            IRouterConfig config = (IRouterConfig)getClass()
                    .getClassLoader()
                    .loadClass(configName)
                    .newInstance();

            config.configure(router);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            LOG.severe("Failed to load router configurator " + configName + ": " + e.getMessage());
        }

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        LOG.info("Matching " + path);
        Route route = router.match(path);
        LOG.info("Matched to " + route);
        if (route != null) {
            RequestDispatcher dispatcher = request.getServletContext().getNamedDispatcher(route.handler);
            if (dispatcher != null) {
                FilterChain head = new FilterChainTail(dispatcher);
                for (Class<?> filterClass : route.filters) {
                    try {
                        Filter filter = (Filter)filterClass.newInstance();
                        head = new FilterChainNode(filter, head);
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                head.doFilter(request, response);
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }

}