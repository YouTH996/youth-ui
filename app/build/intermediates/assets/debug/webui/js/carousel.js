
var carousel = document.querySelector("#gesture-area");
var imgLis,imgCount,prev,next,timer;
var now;

var windowWidth;

var deltaX; // 手指的偏移量
var startX; // 触摸开始时候手指位置 
var startTime;
var clicktime = +new Date();

$(function(){
    // 初始化
    wallpaperInit(0);

    window.addEventListener("resize",()=>{ 
        wallpaperInit(Math.random());
    });
    // 事件监听
    carousel.addEventListener("touchstart", touchstartHandler, false);
    carousel.addEventListener("touchmove", touchmoveHandler, false);
    carousel.addEventListener("touchend", touchendHandler, false);
});

function wallpaperInit(time) {
    updateWallpaper(Math.random());

    var mode = window.carJsBridge.config("UI_THEME");
    if(mode < 0) mode = 1;
    now = parseInt(localStorage.getItem("CUR_WALLPAPER_"+ mode)) || 0;

    imgLis = document.querySelectorAll("#gesture-area li");
    imgCount = imgLis.length;
    if(now > imgCount-1) now = 0;
    prev = now == 0 ? imgCount-1 : now-1;
    next = now == (imgCount-1) ? 0 : now+1;
    // 获取屏幕宽度
    windowWidth = document.documentElement.clientWidth;
    // 设置li的默认值
    for (var i = 0; i < imgLis.length; i++) {
        imgLis[i].style.webkitTransform = "translateX(" + windowWidth + "px)";
    }

    // 去掉过渡
    imgLis[now].style.transition = "none";
    imgLis[prev].style.transition = "none";
    imgLis[next].style.transition = "none";

    // 新的位置设置
    imgLis[now].style.webkitTransform = "translateX(0px)";
    imgLis[prev].style.webkitTransform = "translateX(" + (-windowWidth) + "px)";
    imgLis[next].style.webkitTransform = "translateX(" + windowWidth + "px)";
    setNowIdx(now);
    autoCarousel(0);
}

function updateWallpaper(time){
    var wallpapers = Array.from({length: 10}, (val, i) => "wallpaper/"+(i+1)+".jpg"),
        custome_wallpapers = getCustomeWallpaper(),
        custom_wallpaper_mode = window.carJsBridge.config("system_custom_wallpaper_mode") || 0,
        bing_wallpapers =  localStorage.getItem("BING_WALLPAPERS"),
        bing_wallpaper_mode = localStorage.getItem("bing_wallpaper_mode") || 0,
        wallpaper_list;

    
    if(bing_wallpapers && parseInt(bing_wallpaper_mode) == 1){
        if(window.navigator.onLine){
           // window.carJsBridge.showToast("网络不给力！");
            //return;
        }
        wallpaper_list = bing_wallpapers.split(",");
        if(wallpaper_list.length > 0){
            wallpapers = wallpaper_list.map(i => (
                //i = "cache/bing/"+ $.md5(i) + ".png"
                i = "http://liszt/cache/bing/"+ $.md5(i) + ".png"
            ));
        }
    }else if(custome_wallpapers && custom_wallpaper_mode == 1){
        wallpaper_list = custome_wallpapers.split(",");
        if(wallpaper_list.length > 0){
            wallpapers = wallpaper_list.map(i => (
                //i = "wallpapers"+ i
                i = "http://liszt/wallpaper"+ i
            ));
        }
    }
    var html = "";
    for(var i in wallpapers){
        html +=`<li><img src="${wallpapers[i]}" onerror="imgExists(this,'${wallpapers[i]}')" alt=""></li>`;
    }
    $("#gesture-area ul").html(html);
}

function autoCarousel(time){
    var wpStyle = localStorage.getItem("wallpaper_style"),
        interval = wpStyle ? JSON.parse(wpStyle).interval : 0;
    
    clearInterval(timer);
    if(interval > 0){
        timer = setInterval(showNext, 1000 * interval);
    }
}

function showNext() {
    if(window.carJsBridge.config("AutoNaviShow") > 0) return false;
    prev = now;
    now = next;
    next++;
    if (next > (imgCount-1)) {
        next = 0;
    }

    imgLis[next].style.transition = "none";
    imgLis[next].style.webkitTransform = "translateX(" + windowWidth + "px)";
    // 过渡
    imgLis[prev].style.transition = "all 0.3s ease 0s";
    imgLis[now].style.transition = "all 0.3s ease 0s";

    imgLis[prev].style.webkitTransform = "translateX(" + (-windowWidth) + "px)";
    imgLis[now].style.webkitTransform = "translateX(0px)"; 
    setNowIdx(now);
}


function touchstartHandler(event) {
    event.preventDefault();
    if (event.touches.length > 1) {
        return;
    }
    if((+new Date()) - clicktime < 350 ){
        window.carJsBridge.sysBtnEvent("bydAc");
    }else{
        clicktime = +new Date();
    }
    if (global("carousel_wallpaper_show") != 1) {
        return;
    }
    //if(window.carJsBridge.config("AutoNaviShow") > 0) return false;
    //$(".carousel li").css("-webkit-mask-image","linear-gradient(to right,transparent,rgba(0,0,0,1),transparent)");
    // 记录偏移量
    deltaX = event.touches[0].clientX;
    startX = event.touches[0].clientX;
    // 去掉过渡
    imgLis[now].style.transition = "none";
    imgLis[prev].style.transition = "none";
    imgLis[next].style.transition = "none";
    // 记录时间戳
    startTime = new Date();
}

function touchmoveHandler(event) {
    event.preventDefault();
    if (event.touches.length > 1 || global("carousel_wallpaper_show") != 1) {
        return;
    }
    //if(window.carJsBridge.config("AutoNaviShow") > 0) return false;
    // 得到实时坐标
    var clientX = event.touches[0].clientX;
    // 改变图片的位置
    imgLis[now].style.webkitTransform = "translateX(" + (clientX - deltaX) + "px)";
    imgLis[next].style.webkitTransform = "translateX(" + (windowWidth + clientX - deltaX) + "px)";
    imgLis[prev].style.webkitTransform = "translateX(" + (-windowWidth + clientX - deltaX) + "px)";

}

// 300ms
function touchendHandler(event) {
    // 阻止默认事件
    event.preventDefault();
    if(global("carousel_wallpaper_show") != 1){
        return;
    }
    //$(".carousel li").css("-webkit-mask-image","");
    // 判断滑动是否成功
    var distance = event.changedTouches[0].clientX - startX;
    var time = new Date() - startTime;
    // 什么时候认为滑动成功？ 1  你向右边滑动距离超过屏幕1/5  2  滑动整个过程小于500ms


    if (distance >= windowWidth / 10 && time < 1000) {
        //console.log("向右滑动成功");
        next = now;
        now = prev;
        prev--;
        if (prev < 0) {
            prev = (imgCount-1);
        }
        // 加过渡
        imgLis[now].style.transition = "all 0.3s ease 0s";
        imgLis[next].style.transition = "all 0.3s ease 0s";
        // 平移
        imgLis[now].style.webkitTransform = "translateX(0px)";
        imgLis[next].style.webkitTransform = "translateX(" + windowWidth + "px)";
        setNowIdx(now);
    } else if (distance < -windowWidth / 10 && time < 1000) {
        showNext();
    } else {
        // 滑动不成功
        console.log("滑动不成功");
        // 过渡
        imgLis[now].style.transition = "all 0.3s ease 0s";
        imgLis[prev].style.transition = "all 0.3s ease 0s";
        imgLis[next].style.transition = "all 0.3s ease 0s";

        // 移动
        imgLis[now].style.webkitTransform = "translateX(0px)";
        imgLis[next].style.webkitTransform = "translateX(" + windowWidth + "px)";
        imgLis[prev].style.webkitTransform = "translateX(" + (-windowWidth) + "px)";
    }
}

function setNowIdx(id){
    var mode = window.carJsBridge.config("UI_THEME");
    if(mode < 0) mode = 1;
    localStorage.setItem("CUR_WALLPAPER_"+ mode,id);
}

function imgExists(e,imgUrl){
    e.src = "wallpaper/1.jpg";
    setTimeout(function(){
        e.src = imgUrl;
    }, 5000);
}

function getCustomeWallpaper(){
    var wallpapers;
    var mode = window.carJsBridge.config("UI_THEME");
    switch (mode){
        case 0:
            wallpapers = window.carJsBridge.local("WALLPAPERS_LIGHT");
            break;
        case 1:
            wallpapers = window.carJsBridge.local("WALLPAPERS_DARK");
            break;
    }
    if(!wallpapers){
        wallpapers = window.carJsBridge.local("WALLPAPERS");
    }
    return wallpapers;
}