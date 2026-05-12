/* * 
 *微软天气卡片动态背景图获取
 *@理思特 lixinb@qq.com
 */
 const imgPre = "https://assets.msn.com/weathermapdata/1/static/background/v2.0/jpg/",
 iconPre = "https://assets.msn.cn/weathermapdata/1/static/weather/Icons/taskbar_v10/Condition_Card/",
 weatherBG = {
 sunnyImageUrl: "sunny.jpg",
 sunnyNightImageUrl: "clear_night.jpg",
 rainImageUrl: "rain_2.jpg",
 rainNightImageUrl: "rain_1.jpg",
 snowImageUrl: "snow_2.jpg",
 snowNightImageUrl: "snow_1.jpg",
 dustImageUrl: "duststorm_2.jpg",
 dustNightImageUrl: "duststorm_1.jpg",
 cloudyImageUrl: "cloudy_2.jpg",
 cloudyNightImageUrl: "cloudy_1.jpg",
 stormImageUrl: "thunderstorms_2.jpg",
 stormNightImageUrl: "thunderstorms_1.jpg",
 fogImageUrl: "light_fog.jpg",
 fogNightImageUrl: "hazy_night.jpg",
 sunnySunsetImageUrl: "partlysunny_sunset.jpg",
 cloudySunsetImageUrl: "mostcloudy_sunset.jpg",
 hazyImageUrl: "hazy_day.jpg",
 partlySunnyImageUrl: "partlysunny_day.jpg"
},
skyCode = {
 17: "8",
 31: "31",
 49: "14",
 41: "14",
 35: "8",
 4: "4",
 19: "19",
 22: "14",
 23: "23",
 34: "7",
 47: "20",
 60: "7",
 81: "26",
 11: "10",
 37: "10",
 38: "10",
 69: "10",
 70: "10",
 71: "10",
 72: "10",
 92: "91",
 32: "5",
 2: "2",
 101: "1",
 42: "15",
 33: "6",
 61: "6",
 62: "6",
 87: "6",
 88: "6",
 93: "6",
 94: "6",
 95: "6",
 96: "6",
 67: "27",
 68: "27",
 18: "9",
 21: "9",
 36: "9",
 45: "9",
 48: "9",
 63: "9",
 64: "9",
 29: "29",
 102: "28",
 44: "44",
 46: "19",
 80: "50",
 76: "24",
 75: "24",
 83: "77",
 84: "78",
 85: "24",
 86: "24",
 39: "39"
},
weatherIcon = {
 23: "HeavyDrizzle",
 40: "HeavyDrizzle",
 26: "SnowShowersDayV2",
 6: "BlowingHailV2",
 5: "CloudyV3",
 20: "LightSnowV2",
 91: "WindyV2",
 27: "ThunderstormsV2",
 10: "FreezingRainV2",
 77: "RainSnowV2",
 12: "Haze",
 13: "HeavyDrizzle",
 39: "Fair",
 24: "RainSnowV2",
 78: "RainSnowShowersNightV2",
 9: "FogV2",
 3: "PartlyCloudyDayV3",
 43: "IcePelletsV2",
 16: "IcePellets",
 8: "LightRainV2",
 15: "HeavySnowV2",
 28: "ClearNightV3",
 30: "PartlyCloudyNightV2",
 14: "ModerateRainV2",
 1: "SunnyDayV3",
 7: "BlowingSnowV2",
 50: "RainShowersNightV2",
 82: "LightSnowShowersNight",
 81: "LightSnowShowersDay",
 2: "MostlySunnyDay",
 29: "MostlyClearNight",
 4: "MostlyCloudyDayV2",
 31: "MostlyCloudyNightV2",
 19: "LightRainV3",
 17: "LightRainShowerDay",
 53: "N422Snow",
 52: "Snow",
 25: "Snow",
 44: "LightRainShowerNight",
 65: "HailDayV2",
 73: "HailDayV2",
 74: "HailNightV2",
 79: "RainShowersDayV2",
 89: "HazySmokeV2",
 90: "HazeSmokeNightV2_106",
 66: "HailNightV2",
 59: "WindyV2",
 56: "ThunderstormsV2",
 58: "FogV2",
 54: "HazySmokeV2",
 55: "Dust1",
 57: "Haze"
};

function isSunriseSunset(e, a, t) {
 const r = 36e5,
     n = 18e5,
     i = e.getTime(),
     o = a.getTime(),
     l = t.getTime();
 return o + n > i && o - r < i || l + n > i && l - r < i
}

function getSunriseSunsetSkyCode(e, a = !1) {
 if (!a) return e;
 switch (e) {
     case "3":
     case "30":
         return "301";
     case "4":
     case "31":
         return "401";
     default:
         return e
 }
}

function contentBackground(e) {
 switch (e) {
     case "1":
     case "2":
     case "101":
         return weatherBG.sunnyImageUrl;
         break;
     case "3":
     case "4":
         return weatherBG.partlySunnyImageUrl;
         break;
     case "28":
     case "29":
     case "102":
         return weatherBG.sunnyNightImageUrl;
         break;
     case "8":
     case "10":
     case "11":
     case "13":
     case "14":
     case "17":
     case "19":
     case "22":
     case "23":
     case "24":
     case "69":
     case "71":
     case "75":
     case "77":
     case "79":
     case "83":
     case "85":
         return weatherBG.rainImageUrl;
         break;
     case "35":
     case "37":
     case "38":
     case "40":
     case "41":
     case "44":
     case "46":
     case "49":
     case "50":
     case "51":
     case "70":
     case "72":
     case "76":
     case "78":
     case "80":
     case "84":
     case "86":
         return weatherBG.rainNightImageUrl;
         break;
     case "7":
     case "15":
     case "16":
     case "20":
     case "25":
     case "26":
     case "59":
     case "81":
         return weatherBG.snowImageUrl;
         break;
     case "34":
     case "42":
     case "43":
     case "47":
     case "52":
     case "53":
     case "60":
     case "82":
         return weatherBG.snowNightImageUrl;
         break;
     case "6":
     case "61":
     case "87":
     case "91":
     case "93":
     case "95":
         return weatherBG.dustImageUrl;
         break;
     case "33":
     case "62":
     case "88":
     case "92":
     case "94":
     case "96":
         return weatherBG.dustNightImageUrl;
         break;
     case "5":
         return weatherBG.cloudyImageUrl;
         break;
     case "30":
     case "31":
     case "32":
         return weatherBG.cloudyNightImageUrl;
         break;
     case "27":
     case "67":
         return weatherBG.stormImageUrl;
         break;
     case "54":
     case "68":
         return weatherBG.stormNightImageUrl;
         break;
     case "9":
     case "18":
     case "21":
     case "63":
     case "65":
     case "73":
     case "89":
         return weatherBG.fogImageUrl;
         break;
     case "36":
     case "39":
     case "45":
     case "48":
     case "64":
     case "66":
     case "74":
     case "90":
         return weatherBG.fogNightImageUrl;
         break;
     case "301":
         return weatherBG.sunnySunsetImageUrl;
         break;
     case "401":
         return weatherBG.cloudySunsetImageUrl;
         break;
     case "12":
         return weatherBG.hazyImageUrl;
 }
}

function getBackgroudImage(code, isNight){
 var bgImg = contentBackground(getSunriseSunsetSkyCode(code, isNight));
 return imgPre + bgImg
}

function getIcon(code){
 var icon = weatherIcon[code] || weatherIcon[skyCode[code]];
 return iconPre + icon + ".svg";
}
function getWeaIconByCode(e, t){
 var a = "";
 switch (e = parseInt(e)){
     case 1:
     case 2:
     case 3:
         a = "SunnyDayV3";
         break;
     case 4:
     case 6:
     case 7:
         a = t ? "MostlyCloudyNightV2" : "MostlyCloudyDayV2";
         break;
     case 5:
     case 37:
     case 70:
     case 71:
     case 72:
         a = "Haze";
         break;
     case 8:
     case 75:
         a = "CloudyV3";
         break;
     case 11:
     case 68:
     case 69:
     case 73:
     case 74:
         a = "FogV2";
         break;
     case 12:
     case 13:
     case 14:
     case 39:
     case 40:
         a = t ? "RainShowersNightV2" : "RainShowersDayV2";
         break;
     case 15:
     case 16:
     case 17:
     case 41:
     case 42:
     case 45:
         a = "ThunderstormsV2";
         break;
     case 18:            
     case 47:
     case 57:
     case 49:
     case 50:
     case 59:
     case 60:
     case 51:
     case 61:
         a = "ModerateRainV2";
         break;
     case 19:
     case 20:
     case 21:
     case 22:
     case 23:
     case 43:
     case 44:
     case 76:
     case 53:
     case 62:
         a = "Snow";
         break;
     case 25:
         a = "RainSnowV2";
         break;
     case 26:
         a = "FreezingRainV2";
         break;
     case 32:
         a = "WindyV2";
         break;
     case 33:
     case 34:
         a = "ClearNightV3";
         break;
     case 35:
     case 36:
     case 38:
         a = "MostlyCloudyNightV2";
         break;
     case 46:
         a = "LightRainV2";
         break;
     case 48:
     case 58:
         a = "HeavyDrizzle";
         break;
     case 52:
         a = "LightSnowV2";
         break;
     case 54:
     case 63:
     case 55:
     case 64:
         a = "HeavySnowV2";
         break;
     case 56:
     case 67:
         a = "BlowingHailV2";
         break;
     case 65:
     case 66:
         a = "Dust1";
         break;
 }
 return "/img/weather/" + a + ".svg";
}
function getWeaIconByZmCode(e,t){
    var a = "";
    switch (e = parseInt(e)){
        case 0:
            a = t ? "ClearNightV3" : "SunnyDayV3";
            break;
        case 1:
            a = t ? "MostlyCloudyNightV2" : "MostlyCloudyDayV2";
            break;
        case 2:
            a = "CloudyV3";
            break;
        case 3:
            a = t ? "LightRainShowerNight" : "LightRainShowerDay";
            break;
        case 4:
        case 5:
            a = "ThunderstormsV2";
            break;
        case 6:
            a = "RainSnowV2";
            break;
        case 7:
        case 21:
            a = "LightRainV2";
            break;
        case 8:
        case 22:
            a = "HeavyDrizzle";
            break;
        case 9:
        case 10:
        case 11:
        case 12:
        case 23:
        case 24:
        case 25:
            a = "ModerateRainV2";
            break;
        case 13:
            a = t ? "LightSnowShowersNight" : "LightSnowShowersDay";
            break;
        case 14:
        case 26:
            a = "LightSnowV2";
            break;
        case 15:
        case 27:
            a = "Snow";
            break;
        case 16:
        case 17:
        case 28:
            a = "HeavySnowV2";
            break;
        case 18:
            a = "FogV2";
            break;
        case 19:
            a = "FreezingRainV2";
            break;
        case 20:
        case 29:
        case 30:
        case 31:
            a = "Dust1";
            break;
        case 53:
            a = "Haze";
            break;
    }
    return "/img/weather/" + a + ".svg";
}
function formatTime(time) {
    const date = new Date();
    const year = date.getFullYear(); 
    const month = date.getMonth() + 1; 
    const day = date.getDate(); 
    return `${year}-${month}-${day} ${time}`; 
}