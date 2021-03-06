package com.dounine.japi.core.valid.comm;

import com.alibaba.fastjson.JSON;
import com.dounine.japi.common.JapiPattern;
import com.dounine.japi.core.IField;
import com.dounine.japi.core.IFieldDoc;
import com.dounine.japi.core.IParameter;
import com.dounine.japi.core.IType;
import com.dounine.japi.core.impl.BuiltInJavaImpl;
import com.dounine.japi.core.impl.ParameterImpl;
import com.dounine.japi.core.impl.TypeConvert;
import com.dounine.japi.core.impl.TypeImpl;
import com.dounine.japi.exception.JapiException;
import com.dounine.japi.serial.request.IRequest;
import com.dounine.japi.serial.request.RequestImpl;
import com.dounine.japi.core.valid.IMVC;
import com.dounine.japi.core.valid.IValid;
import com.dounine.japi.serial.response.IResponse;
import com.dounine.japi.serial.response.ResponseImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lake on 17-2-20.
 */
public class DefaultValid implements IValid {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultValid.class);

    private String javaFilePath;

    @Override
    public List<IMVC> getTypes() {
        return null;
    }

    @Override
    public IParameter getParameter(String parameterStr, List<String> docsStrs) {
        ParameterImpl parameter = new ParameterImpl();
        String[] typeAndName = parameterStr.split(StringUtils.SPACE);

        String description = "";
        if (null != docsStrs && docsStrs.size() > 0) {
            for (String doc : docsStrs) {
                Pattern pattern = JapiPattern.getPattern("[*]\\s*[@]\\S*\\s*" + typeAndName[1]);//找到action传进来的注解信息
                Matcher matcher = pattern.matcher(doc);
                if (matcher.find()) {
                    description = doc.substring(matcher.end()).trim();
                    break;
                }
            }
        }

        if (BuiltInJavaImpl.getInstance().isBuiltInType(typeAndName[0])) {
            List<IRequest> requestFields = new ArrayList<>();
            RequestImpl requestField = new RequestImpl();
            if (description.trim().startsWith("{")) {
                DocObj docObj = JSON.parseObject(description, DocObj.class);
                requestField.setConstraint(docObj.getCon());
                requestField.setDefaultValue(docObj.getDef());
                requestField.setDescription(docObj.getDes());
                requestField.setRequired(docObj.isReq());
            } else {
                requestField.setRequired(false);
                requestField.setDescription(description);
                requestField.setDefaultValue("");
            }
            requestField.setName(typeAndName[1]);
            requestField.setType(typeAndName[0].toLowerCase());
            requestFields.add(requestField);
            parameter.setRequestFields(requestFields);
        } else {
            TypeImpl type = new TypeImpl();
            type.setJavaFile(new File(javaFilePath));
            type.setJavaKeyTxt(typeAndName[0]);

            RequestImpl requestField = new RequestImpl();
            if (description.trim().startsWith("{")) {
                DocObj docObj = JSON.parseObject(description, DocObj.class);
                requestField.setConstraint(docObj.getCon());
                requestField.setDefaultValue(docObj.getDef());
                requestField.setDescription(docObj.getDes());
                requestField.setRequired(docObj.isReq());
            }else{
                requestField.setRequired(false);
                requestField.setDefaultValue("");
                if (StringUtils.isBlank(description)) {
                    requestField.setDescription(type.getName());
                }
            }
            requestField.setName(typeAndName[1]);
            requestField.setType(typeAndName[0].toLowerCase());
            requestField.setFields(getChildFields(type.getFields()));
            List<IRequest> requestFields = new ArrayList<>();
            requestFields.add(requestField);
            parameter.setRequestFields(requestFields);
        }
        return parameter;
    }

    private List<IRequest> getChildFields(List<IField> iFields) {
        List<IRequest> requests = new ArrayList<>();
        for (IField iField : iFields) {
            RequestImpl requestField = new RequestImpl();
            String description = "";
            requestField.setName(iField.getName());
            requestField.setDefaultValue("");
            requestField.setRequired(false);
            if (!"$this".equals(iField.getType())) {
                requestField.setType(TypeConvert.getHtmlType(iField.getType()));
            } else {
                requestField.setType("$this");
            }
            for (IFieldDoc fieldDoc : iField.getDocs()) {
                if (StringUtils.isBlank(fieldDoc.getValue())) {
                    description = fieldDoc.getName();
                    break;
                }
            }
            for (IFieldDoc fieldDoc : iField.getDocs()) {
                if (fieldDoc.getName().equals("req")) {
                    if (StringUtils.isBlank(fieldDoc.getValue())) {
                        throw new JapiException("* @req 注释不能为空[true,false]");
                    } else if (!"true".equals(fieldDoc.getValue()) && !"false".equals(fieldDoc.getValue())) {
                        throw new JapiException("* @req 注释只能为true|false");
                    }
                    requestField.setRequired(Boolean.parseBoolean(fieldDoc.getValue()));
                } else if (fieldDoc.getName().equals("des")) {
                    description = fieldDoc.getValue();
                } else if (fieldDoc.getName().equals("def")) {
                    requestField.setDefaultValue(fieldDoc.getValue());
                } else if (fieldDoc.getName().equals("con")) {
                    requestField.setConstraint(fieldDoc.getValue());
                }
            }
            requestField.setDescription(description);
            if (iField.getFields() != null) {
                requestField.setFields(getChildFields(iField.getFields()));
            }
            requests.add(requestField);
        }
        return requests;
    }


    public String getJavaFilePath() {
        return javaFilePath;
    }

    public void setJavaFilePath(String javaFilePath) {
        this.javaFilePath = javaFilePath;
    }

}
