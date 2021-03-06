package cse.swengineering.smtm.menus;

import cse.swengineering.smtm.SmtmApplication;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private List<Diet> dietList = new ArrayList<>();
    private List<Menu> menuList = new ArrayList<>();

    public MenuService(MenuRepository menuRepository) throws IOException, URISyntaxException {
        this.menuRepository = menuRepository;
    }

    public List<Diet> getDietList() {
        return dietList;
    }
    public void setDietList(List<Diet> dietList) {
        this.dietList = dietList;
    }

    public Diet getDiet(LocalDate date){
        for(Diet diet : dietList){
            if(diet.getDate().equals(date))
                return diet;
        }
        return null;
    }

    public void init() throws IOException, URISyntaxException {
        menuList = menuRepository.findAll();

        final String LOCAL_DATE_REGEX = "^\\d{4}-\\d{2}-\\d{2}$";
        File file = new File(SmtmApplication.class.getClass().getResource("/data2.txt").toURI());
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        List<String> splitByDate = new ArrayList<>();

        // 텍스트 파일에서 모든 데이터 읽고 날짜별로 구분하여 splitByDate에 저장
        line = bufferedReader.readLine();
        while (true) {
            if (line == null)
                break;
            else {
                String concat = line;
                line = bufferedReader.readLine();
                while (line != null && !line.matches(LOCAL_DATE_REGEX)) {
                    concat = concat + "," + line;
                    line = bufferedReader.readLine();
                }
                splitByDate.add(concat);
            }
        }

        for(String str : splitByDate){
            Diet diet = new Diet();
            diet.setDate(LocalDate.parse(str.substring(0, 10)));
            dietList.add(diet);
        }

        // splitByDate를 순회하며 하나하나를 Diet으로 변환
        List<Main> mains = new ArrayList<>();
        for (String str : splitByDate) {
            int index = 2;
            String[] split = str.split(",");
            boolean flag = false;
            for (int i = 0; i < 3; i++) {
                if(index >= split.length) {
                    break;
                }

                Main mainA = new Main();
                mainA.setCalories(split[index]);
//                mainA.setType("A");
                while (++index < split.length && !split[index].contains("메인")) { // index 검사와 동시에 증가
                    mainA.getMenus().add(getMenu(split[index]));
                }
                mains.add(mainA);

                if(index >= split.length) {
                    Main mainC = new Main();
//                    mainC.setType("C");
                    ++index;
                    mains.add(mainC);
                    break;
                }

                Main mainC = new Main();
//                mainC.setType("C");
                if (split[index].equals("메인C")) {
                    mainC.setCalories(split[++index]);
                    while (++index < split.length && !split[index].contains("메인")) {
                        mainC.getMenus().add(getMenu(split[index]));
                    }
                    ++index;
                    mains.add(mainC);
                }
                else {
                    ++index;
                    mains.add(mainC);
                }

                if(index >= split.length) {
                    break;
                }
            }
        }

        int index = 0;
        for(Diet diet : dietList){
            Map<String, Main> main = new HashMap<>();
            main.put("A", mains.get(index++));
            main.put("C", mains.get(index++));
            diet.setBreakfastMains(main);
            main = new HashMap<>();
            main.put("A", mains.get(index++));
            main.put("C", mains.get(index++));
            diet.setLunchMains(main);
            main = new HashMap<>();
            main.put("A", mains.get(index++));
            main.put("C", mains.get(index++));
            diet.setDinnerMains(main);
        }

        // debug
//        for(Diet diet : dietList){
//            diet.print();
//        }

    }

    private Menu getMenu(String korName) {
        for(Menu menu : menuList){
            if(menu.getKorName().equals(korName))
                return menu;
        }
        return null;
    }

/*    public void getDietInformation() throws IOException {
        final String RE_INCLUDE_KOREAN = ".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*";
        final String RE_INCLUDE_ENGLISH = ".*[a-zA-Z]+.*";
        final String RE_KOREAN = "^[가-힣\\s]*$";
        final String RE_DAY = "[0-9]+\\([가-힣]\\)";

        URL url = new URL("http://dorm.cnu.ac.kr/html/kr/sub03/sub03_0304.html");
        URLConnection con = url.openConnection();
        InputStream is = con.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        StringBuilder builder = new StringBuilder();

        // 태그까지 모조리 긁어오기
        while ((line = br.readLine()) != null) {
            if (line.contains("<div class=\"diet_info\">")) {
                builder.append(line);
                line = br.readLine();
                builder.append(line);
            }
            if (line.contains("<table class=\"default_view diet_table\">"))
                while (!(line = br.readLine()).contains("</table>"))
                    if (line.matches(RE_INCLUDE_KOREAN) || line.matches(RE_INCLUDE_ENGLISH))
                        builder.append(line);
        }

        // <"diet_info">에 시작 날짜와 끝 날짜가 있다
        String rawData = builder.toString();
        // 무의미한 공백 제거
        rawData = rawData.replace("\t", "");
        // 시작 날짜와 끝 날짜 얻기
        int dateIndex = rawData.indexOf("<p>202");
        String stringDate = rawData.substring(dateIndex + 3, dateIndex + 13);

        // <br>태그 기준으로 한 번 잘라서 다듬고
        String[] splitByTag = rawData.split("<br />");
//        for(String str : splitByTag)
//            System.out.println(str);

        // 다시 합친 뒤
        String data = convertObjectArrayToString(splitByTag, ".");
//        System.out.println(data);
        // 날짜별로 다 쪼개기
        String[] splitByDay = data.split(RE_DAY);
        List<String> strList = new ArrayList<>();

        for (int i = 1; i <= 7; i++) {
            // 메뉴별로s 다 쪼개서
            String[] splitByMenu = splitByDay[i].split("\\.");
            for (String menuStr : splitByMenu) {
                if (menuStr.contains("메인A") || menuStr.contains("메인 C") || menuStr.contains("메인C")) { // 메인A or 메인C
                    //                    diet.getMenus().add(new Menu(menuStr.substring(menuStr.indexOf("메인"), menuStr.indexOf("kcal]") + "kcal]".length())));
                    String mainAndCalories = menuStr.substring(menuStr.indexOf("메인"), menuStr.indexOf("kcal]") + "kcal]".length());
                    mainAndCalories = mainAndCalories.replace(" ", "");
                    String replace = mainAndCalories.replace("[", " ");
                    replace = replace.replace("]", "");
                    String[] split = replace.split(" ");
                    String main = split[0];
                    String cal = split[1];
                    strList.add(main);
                    strList.add(cal);
                } else if (menuStr.matches(RE_INCLUDE_KOREAN) || menuStr.matches(RE_INCLUDE_ENGLISH)) {
                    if (menuStr.startsWith(" "))
                        menuStr = menuStr.substring(findFirstChar(menuStr));
                    //                    diet.getMenus().add(new Menu(menuStr));
                    strList.add(menuStr);
                }
            }
        }

        strList = strList.stream().filter(c -> {
            if (c.contains("<") || c.contains(">"))
                return false;
            return true;
        }).collect(Collectors.toList());

//        strList.forEach(System.out::println);

        // 아 아침점심저녁 메인A 최소 3번
        boolean flag = false;
        ListIterator<String> iterator = strList.listIterator();
        for (int i = 0; i < 7; i++) {
            Diet diet;
            int countMainA = 0;
            String name = null;
            diet = new Diet();
            diet.setDate(LocalDate.parse(stringDate));
            stringDate = addDay(stringDate);
            Main main = new Main();
            main.setType("A");
            boolean forMainC = false;

            while (true) {
                countMainA++;
                if (countMainA >= 6) break;
                while (iterator.hasNext() && !(name = iterator.next()).contains("메인")) {
                    if (name.contains("kcal")) {
                        name = name.substring(0, name.indexOf("k"));
                        main.setCalories(name);
                    } else {
                        main.getMenus().add(new Menu(name));
                    }
                }
                diet.getMainsKOR().add(main);
                if (iterator.hasNext() && name.equals("메인C")) {
                    forMainC = true;
                    main = new Main();
                    main.setType("C");
                    while (!(name = iterator.next()).contains("메인")) {
                        if (name.contains("kcal")) {
                            name = name.substring(0, name.indexOf("k"));
                            main.setCalories(name);
                        } else
                            main.getMenus().add(new Menu(name));
                    }
                    diet.getMainsKOR().add(main);
                } else if (iterator.hasNext() && name.equals("메인A")) {
                    countMainA++;
                    main = new Main();
                    main.setType("A");
                    while (iterator.hasNext() && !(name = iterator.next()).contains("메인")) {
                        if (name.contains("kcal")) {
                            name = name.substring(0, name.indexOf("k"));
                            main.setCalories(name);
                        } else
                            main.getMenus().add(new Menu(name));
                    }
                    diet.getMainsENG().add(main);
                }
                if (forMainC) {
                    forMainC = false;
                    main = new Main();
                    main.setType("C");
                    while (!(name = iterator.next()).contains("메인")) {
                        if (name.contains("kcal")) {
                            name = name.substring(0, name.indexOf("k"));
                            main.setCalories(name);
                        } else
                            main.getMenus().add(new Menu(name));
                    }
                    diet.getMainsENG().add(main);
                }
            }
            dietList.add(diet);
        }

    }
//        dietList.forEach(System.out::println);


    private int findFirstChar(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ' ')
                return i;
        }
        return -1;
    }

    // string array to string
    private static String convertObjectArrayToString(Object[] arr, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : arr)
            sb.append(obj.toString()).append(delimiter);
        return sb.substring(0, sb.length() - 1);
    }

    private String addDay(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sdf.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //Incrementing the date by 1 day
        c.add(Calendar.DAY_OF_MONTH, 1);
        return sdf.format(c.getTime());
    }*/
}
