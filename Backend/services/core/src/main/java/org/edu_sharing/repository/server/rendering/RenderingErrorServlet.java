package org.edu_sharing.repository.server.rendering;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.HttpException;
import org.edu_sharing.repository.server.tools.I18nServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class RenderingErrorServlet extends HttpServlet {
    private static Logger logger = Logger.getLogger(RenderingErrorServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RenderingException exception = (RenderingException) req
                .getAttribute("javax.servlet.error.exception");
        if(exception!=null) {
            logger.warn(exception.getMessage(), exception);
            if (exception.getNested() != null){
                logger.warn(exception.getNested().getMessage(), exception.getNested());
            }
        }
        String html = errorToHTML(req, exception);

        resp.setHeader("Content-Type","text/html");
        resp.setStatus(exception!=null ? exception.getStatusCode() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getOutputStream().write(html.getBytes("UTF-8"));
    }

    public static String errorToHTML(HttpServletRequest req, RenderingException exception) {
        try {
            if(req==null && Context.getCurrentInstance()!=null){
                req=Context.getCurrentInstance().getRequest();
            }
            File index = new File(Context.getGlobalContext().getRealPath("rendering-error.html"));
            String html = FileUtils.readFileToString(index);
            if(exception!=null) {
                String exceptionName="";
                if (exception.getNested() != null) {
                    exceptionName = exception.getNested().getClass().getSimpleName();
                }
                html = html.replace("{{statusCode}}", exception.getStatusCode() + "");
                html = html.replace("{{message}}", I18nServer.getTranslationDefaultResourcebundleNoException("rendering_error_" + exception.getI18nName()));
                html = html.replace("{{technicalMessage}}",
                        (exception.getNested()!=null && exception.getNested() instanceof HttpException) ?
                                exception.getTechnicalDetail() :
                                "");
                html = html.replace("{{exception}}", exceptionName);
            }
            else{
                String exceptionName=req.getParameter("exception");
                String i18n=req.getParameter("i18n");
                html = html.replace("{{statusCode}}", HttpServletResponse.SC_INTERNAL_SERVER_ERROR+"");
                html = html.replace("{{message}}", I18nServer.getTranslationDefaultResourcebundleNoException("rendering_error_" + i18n));
                html = html.replace("{{technicalMessage}}","");
                html = html.replace("{{exception}}", exceptionName);
            }
            return html;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
