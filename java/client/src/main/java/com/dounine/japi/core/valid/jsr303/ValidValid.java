package com.dounine.japi.core.valid.jsr303;

import com.alibaba.fastjson.JSON;
import com.dounine.japi.common.JapiPattern;
import com.dounine.japi.core.IField;
import com.dounine.japi.core.IFieldDoc;
import com.dounine.japi.core.impl.BuiltInJavaImpl;
import com.dounine.japi.core.impl.TypeConvert;
import com.dounine.japi.core.impl.TypeImpl;
import com.dounine.japi.exception.JapiException;
import com.dounine.japi.serial.request.IRequest;
import com.dounine.japi.serial.request.RequestImpl;
import com.dounine.japi.core.valid.IMVC;
import com.dounine.japi.core.valid.jsr303.list.NotBlankValid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lake on 17-2-17.
 */
public class ValidValid implements IMVC {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidValid.class);
    private static final String METHOD_NAME_DOC = "* @methodName ";

    private String javaFilePath;

    public ValidValid(String javaFilePath) {
        this.javaFilePath = javaFilePath;
    }

    @Override
    public String getRequestParamName() {
        return "javax.validation.Valid";
    }

    @Override
    public RequestImpl getRequestField(String parameterStrExcTypeAndName, String typeStr, String nameStr, List<String> docs, File javaFile) {
        RequestImpl requestField = new RequestImpl();
        requestField.setName(nameStr);
        String description = "";
        if (!BuiltInJavaImpl.getInstance().isBuiltInType(typeStr)) {
            TypeImpl typeImpl = new TypeImpl();
            typeImpl.setJavaFile(javaFile);
            typeImpl.setJavaKeyTxt(typeStr);

            List<IField> fields = typeImpl.getFields();
            List<IMVC> imvcs = getJSR303(typeImpl.getSearchFile().getAbsolutePath());
            requestField.setType("object");
            List<IRequest> requestFields = new ArrayList<>();
            if (fields.size() > 0) {
                for (IField iField : fields) {
                    IMVC mvc = null;
                    String anno = null;
                    for (IMVC imvc : imvcs) {
                        List<String> annotations = iField.getAnnotations();
                        for (String annotation : annotations) {
                            String _anno = null;
                            if (-1 != annotation.indexOf("(")) {
                                _anno = annotation.substring(0, annotation.indexOf("("));
                            } else {
                                _anno = annotation;
                            }
                            if (_anno.equals(imvc.getRequestParamName()) || imvc.getRequestParamName().endsWith(_anno)) {
                                anno = annotation;
                                mvc = imvc;
                                break;
                            }
                        }
                    }
                    if (null != mvc) {//找到对应jsr303注解
                        requestFields.add(mvc.getRequestFieldForAnno(anno, iField.getType(), iField.getName(), iField.getDocs(),null));
                    } else {//其它注没有注解
                        if (null != iField.getAnnotations() && iField.getAnnotations().size() > 0) {
                            System.out.println(JSON.toJSONString(iField.getAnnotations()) + "这些注解我都不认识噢.");
                        } else {
                            if (!"$this".equals(iField.getType())) {
                                List<String> _docs = new ArrayList<>();
                                _docs.add(METHOD_NAME_DOC + iField.getName() + " " + iField.getDocs().get(0).getName());
                                for (IFieldDoc fieldDoc : iField.getDocs()) {
                                    if (fieldDoc.getValue() == null) {

                                    } else {
                                        _docs.add("* @" + fieldDoc.getName() + " " + fieldDoc.getValue() + (fieldDoc.getDes() == null ? "" : (" " + fieldDoc.getDes())));
                                    }
                                }
                                RequestImpl _requestField = getRequestField(null,iField.getType(), iField.getName(), _docs,typeImpl.getSearchFile());
                                if(null!=_requestField){
                                    requestFields.add(_requestField);
                                }
                            } else {
                                RequestImpl _requestField = new RequestImpl();
                                _requestField.setName(iField.getName());
                                _requestField.setType("$this");
                                _requestField.setDescription("自身对象");
                                _requestField.setRequired(false);
                                _requestField.setDefaultValue("");
                                requestFields.add(_requestField);
                            }
                        }
                    }
                }
                requestField.setFields(requestFields);
            }
//            if (StringUtils.join(fieldBuffer.toArray(), ",").contains("required:true")) {
//                sb.append(",required:true");
//            }
            requestField.setDefaultValue("");
            if (null != docs && docs.size() > 0) {
                Pattern pattern = JapiPattern.getPattern("[*]\\s*[@]\\S*\\s*" + nameStr + "\\s+");//找到action传进来的注解信息
                for (String doc : docs) {
                    Matcher matcher = pattern.matcher(doc);
                    if (matcher.find()) {
                        String val = matcher.group();
                        if (val.startsWith(METHOD_NAME_DOC)) {
                            description = doc.substring(matcher.end()).trim();
                            break;
                        }
                    }
                }
                for (String doc : docs) {
                    Pattern reqDefConPattern = JapiPattern.getPattern("[*]\\s*[@]\\S*\\s*[a-zA-Z0-9_]\\s+");//找到action传进来的注解信息
                    Matcher matcher = reqDefConPattern.matcher(doc);
                    if (matcher.find()) {
                        String str = matcher.group();
                        if (str.startsWith("* @req")) {
                            String value = (doc.substring(matcher.end()).trim()).trim();
                            if (StringUtils.isBlank(value)) {
                                throw new JapiException("* @req 注释不能为空[true,false]");
                            } else if (!"true".equals(value) && !"false".equals(value)) {
                                throw new JapiException("* @req 注释只能为true|false");
                            }
                            requestField.setRequired(Boolean.parseBoolean(value));
                        } else if (str.startsWith("* @des")) {
                            description = (doc.substring(matcher.end()).trim()).trim();
                        } else if (str.startsWith("* @def")) {
                            String value = (doc.substring(matcher.end()).trim()).trim();
                            requestField.setDefaultValue(value);
                        } else if (str.startsWith("* @con")) {
                            String value = (doc.substring(matcher.end()).trim()).trim();
                            requestField.setConstraint(value);
                        }
                    }
                }
            }
            if(StringUtils.isBlank(description)){
                description = typeImpl.getName();
            }
        }else{
            requestField.setType(TypeConvert.getHtmlType(typeStr));
            requestField.setDefaultValue("");
            requestField.setRequired(false);
            if (null != docs && docs.size() > 0) {
                for (String doc : docs) {
                    Pattern pattern = JapiPattern.getPattern("[*]\\s*[@]\\S*\\s*" + nameStr);//找到action传进来的注解信息
                    Matcher matcher = pattern.matcher(doc);
                    if (matcher.find()) {
                        description = doc.substring(matcher.end()).trim();
                        break;
                    }
                }
            }
        }
        requestField.setDescription(description);
        return requestField;
    }

    public String getJavaFilePath() {
        return javaFilePath;
    }

    public void setJavaFilePath(String javaFilePath) {
        this.javaFilePath = javaFilePath;
    }
}
