package com.mikufans.common.util;

import com.mikufans.common.exception.BusinessException;
import com.mikufans.common.web.AjaxResult;
import com.mikufans.config.RuoYiConfig;
import com.mikufans.config.aspectj.annotation.Excel;
import com.mikufans.config.aspectj.annotation.Excels;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import sun.net.ftp.FtpDirEntry;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

/**
 * excle相关的操作
 * @param <T>
 */
@Slf4j
public class ExcelUtils<T>
{
    /**
     * Excel sheet最大行数，默认65536
     */
    public static final int sheetSize = 65536;
    /**
     * 实体对象
     */
    public Class<T> clazz;
    /**
     * 工作表名称
     */
    private String sheetName;
    /**
     * 导出类型（EXPORT:导出数据；IMPORT：导入模板）
     */
    private Excel.Type type;
    /**
     * 工作薄对象
     */
    private Workbook wb;
    /**
     * 工作表对象
     */
    private Sheet sheet;
    /**
     * 样式列表
     */
    private Map<String, CellStyle> styles;
    /**
     * 导入导出数据列表
     */
    private List<T> list;
    /**
     * 注解列表
     */
    private List<Object[]> fields;

    public ExcelUtils(Class<T> clazz)
    {
        this.clazz = clazz;
    }

    /**
     * 反向解析值 男=0,女=1,未知=2
     *
     * @param propertyValue 参数值
     * @param converterExp  翻译注解
     * @return 解析后值
     * @throws Exception
     */
    public static String reverseByExp(String propertyValue, String converterExp)
    {
        try
        {
            String[] convertSource = converterExp.split(",");
            for (String item : convertSource)
            {
                String[] itemArray = item.split("=");
                if (itemArray[1].equals(propertyValue))
                {
                    return itemArray[0];
                }
            }
        } catch (Exception e)
        {
            throw e;
        }
        return propertyValue;

    }

    public void init(List<T> list, String sheetName, Excel.Type type)
    {
        if (list == null)
            list = new ArrayList<>();
        this.list = list;
        this.sheetName = sheetName;
        this.type = type;
        createExcelField();
        createWorkbook();
    }

    /**
     * 对excel表单默认第一个索引名转换成list
     *
     * @param is 输入流
     * @return 转换后集合
     */
    public List<T> importExcel(InputStream is) throws Exception
    {
        return importExcel(StringUtils.EMPTY, is);
    }


    /**
     * 对excel表单指定表格索引名转换成list
     *
     * @param sheetName   表格索引名
     * @param inputStream 输入流
     * @return 转换后集合
     */
    public List<T> importExcel(String sheetName, InputStream inputStream) throws Exception
    {
        this.type = Excel.Type.IMPORT;
        this.wb = WorkbookFactory.create(inputStream);
        List<T> list = new ArrayList<>();
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName))

            sheet = wb.getSheet(sheetName);    // 如果指定sheet名,则取指定sheet中的内容
        else sheet = wb.getSheetAt(0);      // 如果传入的sheet名不存在则默认指向第1个sheet

        if (sheet == null)
            throw new IOException("文件sheet不存在");
        int rows = sheet.getPhysicalNumberOfRows();

        if (rows > 0)
        {
            //存放excel列的序号和field
            Map<String, Integer> cellMap = new HashMap<>();
            Row heard = sheet.getRow(0);
            for (int i = 0; i < heard.getPhysicalNumberOfCells(); i++)
            {
                Cell cell = heard.getCell(i);
                if (StringUtils.isNotNull(cell))
                {
                    String value = this.getCellValue(heard, i).toString();
                    cellMap.put(value, i);
                } else cellMap.put(null, i);
            }
            //有数据才处理  得到类得所有field
            Field[] allFields = clazz.getDeclaredFields();
            //存放列得序号和field
            Map<Integer, Field> fieldMap = new HashMap<>();

            for (int col = 0; col < allFields.length; col++)
            {
                Field field = allFields[col];
                Excel attr = field.getAnnotation(Excel.class);
                if (attr != null && (attr.type() == Excel.Type.ALL || attr.type() == type))
                {
                    field.setAccessible(true);
                    Integer column = cellMap.get(attr.name());
                    fieldMap.put(column, field);
                }
            }

            for (int i = 1; i < rows; i++)
            {
                //从第二行开始读取  第一行默认时表头
                Row row = sheet.getRow(i);
                T entity = null;
                for (Map.Entry<Integer, Field> entry : fieldMap.entrySet())
                {
                    Object val = this.getCellValue(row, entry.getKey());
                    entity = (entity == null ? clazz.newInstance() : entity);
                    Field field = fieldMap.get(entry.getKey());
                    Class<?> fieldType = field.getType();
                    if (String.class == fieldType)
                    {
                        String s = Convert.toStr(val);
                        if (StringUtils.endsWith(s, ".0"))
                            val = StringUtils.substringBefore(s, ".0");
                        else
                        {
                            String dateFormat = field.getAnnotation(Excel.class).dateFormat();
                            if (StringUtils.isNotEmpty(dateFormat))
                                val = DateUtils.parseDateToStr(dateFormat, (Date) val);
                            else
                                val = Convert.toStr(val);
                        }
                    } else if ((Integer.TYPE == fieldType) || (Integer.class == fieldType))
                        val = Convert.toInt(val);
                    else if ((Long.TYPE == fieldType) || (Long.class == fieldType))
                        val = Convert.toLong(val);
                    else if ((Double.TYPE == fieldType) || (Double.class == fieldType))
                        val = Convert.toDouble(val);
                    else if ((Float.TYPE == fieldType) || (Float.class == fieldType))
                        val = Convert.toFloat(val);
                    else if (BigDecimal.class == fieldType)
                        val = Convert.toBigDecimal(val);
                    else if (Date.class == fieldType)
                    {
                        if (val instanceof String)
                            val = DateUtils.parseDate(val);
                        else if (val instanceof Double)
                            val = DateUtil.getJavaDate((Double) val);
                    }

                    if (StringUtils.isNotNull(fieldType))
                    {
                        Excel attr = field.getAnnotation(Excel.class);
                        String propertyName = field.getName();
                        if (StringUtils.isNotEmpty(attr.targetAttr()))
                            propertyName = field.getName() + "." + attr.targetAttr();
                        else if (StringUtils.isNotEmpty(attr.readConverterExp()))
                            val = reverseByExp(String.valueOf(val), attr.readConverterExp());

                        ReflectUtils.invokeSetter(entity, propertyName, val);
                    }

                }
                list.add(entity);
            }
        }
        return list;
    }


    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public AjaxResult exportExcel(List<T> list, String sheetName)
    {
        this.init(list, sheetName, Excel.Type.EXPORT);
        return exportExcel();
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public AjaxResult importTemplateExcel(String sheetName)
    {
        this.init(null, sheetName, Excel.Type.IMPORT);
        return exportExcel();
    }


    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @return 结果
     */
    public AjaxResult exportExcel()
    {
        OutputStream out = null;
        try
        {
            // 取出一共有多少个sheet.
            double sheetNo = Math.ceil(list.size() / sheetSize);
            for (int index = 0; index <= sheetNo; index++)
            {
                createSheet(sheetNo, index);

                // 产生一行
                Row row = sheet.createRow(0);
                int column = 0;
                // 写入各个字段的列头名称
                for (Object[] os : fields)
                {
                    Excel excel = (Excel) os[1];
                    this.createCell(excel, row, column++);
                }
                if (Excel.Type.EXPORT.equals(type))
                {
                    fillExcelData(index, row);
                }
            }
            String filename = encodingFilename(sheetName);
            out = new FileOutputStream(getAbsoluteFile(filename));
            wb.write(out);
            return AjaxResult.success(filename);
        } catch (Exception e)
        {
            log.error("导出Excel异常{}", e.getMessage());
            throw new BusinessException("导出Excel失败，请联系网站管理员！");
        } finally
        {
            if (wb != null)
            {
                try
                {
                    wb.close();
                } catch (IOException e1)
                {
                    e1.printStackTrace();
                }
            }
            if (out != null)
            {
                try
                {
                    out.close();
                } catch (IOException e1)
                {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 编码文件名
     */
    public String encodingFilename(String filename)
    {
        filename = UUID.randomUUID().toString() + "_" + filename + ".xlsx";
        return filename;
    }

    /**
     * 获取下载路径
     *
     * @param filename 文件名称
     */
    public String getAbsoluteFile(String filename)
    {
        String downloadPath = RuoYiConfig.getDownloadPath() + filename;
        File desc = new File(downloadPath);
        if (!desc.getParentFile().exists())
        {
            desc.getParentFile().mkdirs();
        }
        return downloadPath;
    }



    /**
     * 获取所有字段
     */
    private void createExcelField()
    {
        this.fields = new ArrayList<>();
        List<Field> tempFields = new ArrayList<>();
        tempFields.addAll(Arrays.asList(clazz.getSuperclass().getDeclaredFields()));
        tempFields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        for (Field field : tempFields)
        {
            //单注解
            if (field.isAnnotationPresent(Excel.class))
                putToField(field, field.getAnnotation(Excel.class));

            //多注解
            if (field.isAnnotationPresent(Excels.class))
            {
                Excels attrs = field.getAnnotation(Excels.class);
                Excel[] excels = attrs.value();
                for (Excel excel : excels)
                    putToField(field, excel);
            }
        }
    }

    /**
     * 放到字段集合中
     */
    private void putToField(Field field, Excel attr)
    {
        if (attr != null && (attr.type() == Excel.Type.ALL || attr.type() == type))
        {
            this.fields.add(new Object[]{field, attr});

        }
    }

    /**
     * 创建一个工作簿
     */
    public void createWorkbook()
    {
        this.wb = new SXSSFWorkbook(500);
    }

    /**
     * 获取单元行数据
     *
     * @param row    获取的行
     * @param column 单元格的列
     * @return
     */
    public Object getCellValue(Row row, int column)
    {
        if (row == null)
            return row;

        Object val = "";

        Cell cell = row.getCell(column);
        if (StringUtils.isNotNull(cell))
        {
            if (cell.getCellTypeEnum() == CellType.NUMERIC || cell.getCellTypeEnum() == CellType.FORMULA)
            {
                val = cell.getNumericCellValue();
                if (HSSFDateUtil.isCellDateFormatted(cell))
                    val = DateUtil.getJavaDate((Double) val);
                else
                {
                    if ((Double) val % 1 > 0)
                        val = new DecimalFormat("0.00").format(val);
                    else val = new DecimalFormat("0").format(val);
                }
            } else if (cell.getCellTypeEnum() == CellType.STRING)
                val = cell.getStringCellValue();
            else if (cell.getCellTypeEnum() == CellType.BOOLEAN)
                val = cell.getBooleanCellValue();
            else if (cell.getCellTypeEnum() == CellType.ERROR)
                val = cell.getErrorCellValue();
        }
        return val;
    }

    /**
     * 创建工作表
     *
     * @param sheetNo
     * @param index
     */
    public void createSheet(double sheetNo, int index)
    {
        this.sheet = wb.createSheet();
        this.styles = createStyles(wb);
        // 设置工作表的名称.
        if (sheetNo == 0)
            wb.setSheetName(index, sheetName);
        else wb.setSheetName(index, sheetName + index);
    }

    /**
     * 创建表格样式
     *
     * @param wb 工作薄对象
     * @return 样式列表
     */
    private Map<String, CellStyle> createStyles(Workbook wb)
    {
        // 写入各条记录,每条记录对应excel表中的一行
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        Font dataFont = wb.createFont();
        dataFont.setFontName("Arial");
        dataFont.setFontHeightInPoints((short) 10);
        style.setFont(dataFont);
        styles.put("data", style);

        style = wb.createCellStyle();
        style.cloneStyleFrom(styles.get("data"));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = wb.createFont();
        headerFont.setFontName("Arial");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(headerFont);
        styles.put("header", style);

        return styles;
    }


    /**
     * 创建单元格
     */
    public Cell createCell(Excel attr, Row row, int column)
    {
        // 创建列
        Cell cell = row.createCell(column);
        // 写入列信息
        cell.setCellValue(attr.name());
        setDataValidation(attr, row, column);
        cell.setCellStyle(styles.get("header"));
        return cell;
    }

    /**
     * 填充excel数据
     *
     * @param index 序号
     * @param row 单元格行
     */
    public void fillExcelData(int index, Row row)
    {
        int startNo = index * sheetSize;
        int endNo = Math.min(startNo + sheetSize, list.size());
        for (int i = startNo; i < endNo; i++)
        {
            row = sheet.createRow(i + 1 - startNo);
            // 得到导出对象.
            T vo = (T) list.get(i);
            int column = 0;
            for (Object[] os : fields)
            {
                Field field = (Field) os[0];
                Excel excel = (Excel) os[1];
                // 设置实体类私有属性可访问
                field.setAccessible(true);
                this.addCell(excel, row, vo, field, column++);
            }
        }
    }

    /**
     * 添加单元格
     */
    public Cell addCell(Excel attr, Row row, T vo, Field field, int column)
    {
        Cell cell = null;
        try
        {
            // 设置行高
            row.setHeight((short) (attr.height() * 20));
            // 根据Excel中设置情况决定是否导出,有些情况需要保持为空,希望用户填写这一列.
            if (attr.isExport())
            {
                // 创建cell
                cell = row.createCell(column);
                cell.setCellStyle(styles.get("data"));

                // 用于读取对象中的属性
                Object value = getTargetValue(vo, field, attr);
                String dateFormat = attr.dateFormat();
                String readConverterExp = attr.readConverterExp();
                if (StringUtils.isNotEmpty(dateFormat) && StringUtils.isNotNull(value))
                {
                    cell.setCellValue(DateUtils.parseDateToStr(dateFormat, (Date) value));
                }
                else if (StringUtils.isNotEmpty(readConverterExp) && StringUtils.isNotNull(value))
                {
                    cell.setCellValue(convertByExp(String.valueOf(value), readConverterExp));
                }
                else
                {
                    // 设置列类型
                    setCellVo(value, attr, cell);
                }
            }
        }
        catch (Exception e)
        {
            log.error("导出Excel失败{}", e);
        }
        return cell;
    }

    /**
     * 设置单元格信息
     *
     * @param value 单元格值
     * @param attr 注解相关
     * @param cell 单元格信息
     */
    public void setCellVo(Object value, Excel attr, Cell cell)
    {
        if (Excel.ColumnType.STRING == attr.cellType())
        {
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(StringUtils.isNull(value) ? attr.defaultValue() : value + attr.suffix());
        }
        else if (Excel.ColumnType.NUMERIC == attr.cellType())
        {
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(Integer.parseInt(value + ""));
        }
    }




    /**
     * 创建表格样式
     */
    public void setDataValidation(Excel attr, Row row, int column)
    {
        if (attr.name().indexOf("注：") >= 0)
        {
            sheet.setColumnWidth(column, 6000);
        }
        else
        {
            // 设置列宽
            sheet.setColumnWidth(column, (int) ((attr.width() + 0.72) * 256));
            row.setHeight((short) (attr.height() * 20));
        }
        // 如果设置了提示信息则鼠标放上去提示.
        if (StringUtils.isNotEmpty(attr.prompt()))
        {
            // 这里默认设了2-101列提示.
            setXSSFPrompt(sheet, "", attr.prompt(), 1, 100, column, column);
        }
        // 如果设置了combo属性则本列只能选择不能输入
        if (attr.combo().length > 0)
        {
            // 这里默认设了2-101列只能选择不能输入.
            setXSSFValidation(sheet, attr.combo(), 1, 100, column, column);
        }

    }

    /**
     * 设置 POI XSSFSheet 单元格提示
     *
     * @param sheet 表单
     * @param promptTitle 提示标题
     * @param promptContent 提示内容
     * @param firstRow 开始行
     * @param endRow 结束行
     * @param firstCol 开始列
     * @param endCol 结束列
     */
    public void setXSSFPrompt(Sheet sheet, String promptTitle, String promptContent, int firstRow, int endRow,
                              int firstCol, int endCol)
    {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createCustomConstraint("DD1");
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
        DataValidation dataValidation = helper.createValidation(constraint, regions);
        dataValidation.createPromptBox(promptTitle, promptContent);
        dataValidation.setShowPromptBox(true);
        sheet.addValidationData(dataValidation);
    }


    /**
     * 设置某些列的值只能输入预制的数据,显示下拉框.
     *
     * @param sheet 要设置的sheet.
     * @param textlist 下拉框显示的内容
     * @param firstRow 开始行
     * @param endRow 结束行
     * @param firstCol 开始列
     * @param endCol 结束列
     * @return 设置好的sheet.
     */
    public void setXSSFValidation(Sheet sheet, String[] textlist, int firstRow, int endRow, int firstCol, int endCol)
    {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        // 加载下拉列表内容
        DataValidationConstraint constraint = helper.createExplicitListConstraint(textlist);
        // 设置数据有效性加载在哪个单元格上,四个参数分别是：起始行、终止行、起始列、终止列
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
        // 数据有效性对象
        DataValidation dataValidation = helper.createValidation(constraint, regions);
        // 处理Excel兼容性问题
        if (dataValidation instanceof XSSFDataValidation)
        {
            dataValidation.setSuppressDropDownArrow(true);
            dataValidation.setShowErrorBox(true);
        }
        else
        {
            dataValidation.setSuppressDropDownArrow(false);
        }

        sheet.addValidationData(dataValidation);
    }

    /**
     * 获取bean中的属性值
     *
     * @param vo 实体对象
     * @param field 字段
     * @param excel 注解
     * @return 最终的属性值
     * @throws Exception
     */
    private Object getTargetValue(T vo, Field field, Excel excel) throws Exception
    {
        Object o = field.get(vo);
        if (StringUtils.isNotEmpty(excel.targetAttr()))
        {
            String target = excel.targetAttr();
            if (target.indexOf(".") > -1)
            {
                String[] targets = target.split("[.]");
                for (String name : targets)
                {
                    o = getValue(o, name);
                }
            }
            else
            {
                o = getValue(o, target);
            }
        }
        return o;
    }

    /**
     * 以类的属性的get方法方法形式获取值
     *
     * @param o
     * @param name
     * @return value
     * @throws Exception
     */
    private Object getValue(Object o, String name) throws Exception
    {
        if (StringUtils.isNotEmpty(name))
        {
            Class<?> clazz = o.getClass();
            String methodName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
            Method method = clazz.getMethod(methodName);
            o = method.invoke(o);
        }
        return o;
    }

    /**
     * 解析导出值 0=男,1=女,2=未知
     *
     * @param propertyValue 参数值
     * @param converterExp 翻译注解
     * @return 解析后值
     * @throws Exception
     */
    public static String convertByExp(String propertyValue, String converterExp) throws Exception
    {
        try
        {
            String[] convertSource = converterExp.split(",");
            for (String item : convertSource)
            {
                String[] itemArray = item.split("=");
                if (itemArray[0].equals(propertyValue))
                {
                    return itemArray[1];
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
        return propertyValue;
    }






}
