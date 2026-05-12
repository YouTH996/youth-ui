(function($) {
    var gtime=0;
    var lyric_listener;
    $.fn.jqlyric = function(options) {
        var opts = $.extend({}, $.fn.jqlyric.defaults, options);

        return this.each(function(){
            var o = $.meta ? $.extend({}, opts, $this.data()) : opts;
            var $this=$(this);

            var arrLyric=new Array();   //放存汉字的歌词
            var arrTime=new Array();    //存放时间
            var currentLine=0;          //当前活动的歌词行号

            // 开始解析歌词
            var lyric=o.lyric;
            lyric=lyric.replace(/\]\n/g,'\]\n\|\|');
            lyric=lyric.replace(/\]\s\[/g,'\]\[');
            lyric=lyric.replace(/\]\[/g,'====');
            lyric=lyric.replace(/\[/g,'\|\|\[');
            lyric=lyric.replace(/====/g,'\]\[');

            lyric=lyric.replace(/\|\|\|\|\|\|\|\|/g,'\|\|');
            lyric=lyric.replace(/\|\|\|\|\|\|/g,'\|\|');
            lyric=lyric.replace(/\|\|\|\|/g,'\|\|');

            
            //将歌词解析成数组
            var arrly=lyric.split('||');

            for(var i=0;i<arrly.length;i++){
                var str=arrly[i];

                var left=0; //"["的个数
                var leftAr=new Array();
                for(var k=0;k<str.length;k++){
                    if(str.charAt(k)=="["){
                        leftAr[left]=k;
                        left++;
                    }
                }

                if(left!=0){
                    for(var j=0;j<leftAr.length;j++){
                        var tag=str.charAt(leftAr[j]+1)+str.charAt(leftAr[j]+2);

                        if(tag=='ti'||tag=='ar'||tag=='by'||tag=='al'){
                            // 歌曲特征字段
                            var tmp=str.split(tag+':');
                            tmp=tmp[1].split(']');
                            var text=tmp[0];
                            tag=tag.replace(/by/g,'歌词制作:').replace(/([ti|ar|al])/g,'');
                            arrLyric.push(tag+text);
                            arrTime.push(toSeconds('00:00:00'));   //
                        }else{
                            var text=str.substring(str.lastIndexOf("]")+1);
                            if(text==''){text='&nbsp;';}
                            arrLyric.push(text);              //放歌词
                            arrTime.push(toSeconds(str.substring(leftAr[j]+1,leftAr[j]+6)));   //放时间                            
                        }
                    }
                }
            }

            // 所有歌词按时间顺序排列
            for(var k=0;k<arrTime.length;k++){
                for(var j=0;j<arrTime.length-k;j++){
                    if(arrTime[j]>arrTime[j+1]){
                        temp=arrTime[j];
                        arrTime[j]=arrTime[j+1];
                        arrTime[j+1]=temp;

                        temp=arrLyric[j];
                        arrLyric[j]=arrLyric[j+1];
                        arrLyric[j+1]=temp;
                    }
                }
            }
            
            lyric='';
            for(var i=0;i<arrLyric.length;i++){
                lyric+='<li id="line-'+i+'" >'+arrLyric[i]+'</li>';
            }

            $this.html('<div class="fixed-lyric-box" style="position:relative;height:100%;overflow:hidden"><ul class="lyric-list" style="position:absolute;clear:both;display:block">'+lyric+'</ul></div>');

            var $box=$this.find('.fixed-lyric-box');
            var $list=$this.find('ul.lyric-list');

            var childWindow = $(".widget_iframe")[0].contentWindow.document;
            var childList = $('.music__info--lyric ul',childWindow);
            childList.html(lyric)
            childList.css({'top':'0px'});

            var height=$box.height();
            var lineHeight=$list.height()/arrLyric.length;

            var lyricTop = $("#lyric-container").offset().top;

            $list.css({'left':'0px','top':(height-lineHeight)/2+'px'});
            
            clearInterval(lyric_listener);
            lyric_listener=setInterval(function(){     // 定时检查当前播放进度，作出滚动动作
                var pos=o.getPosition();
                if(arrTime[currentLine]<=pos){  // 当前显示行号时间小于歌曲进度，需要正常向前移动
                    for(var i=currentLine;i<arrLyric.length;i++){
                        if(arrTime[i+1]>pos||i+1==arrLyric.length){
                            var height=$box.height();
                            currentLine=i;
                            
                            $list.find('li.active').removeClass('active');
                            $list.find('li#line-'+currentLine).addClass('active');

                            
                            var distanceFromParentTop,
                                target = $("#line-"+(currentLine+1));
                            if(typeof(target) != "undefined"){
                                var targetOffset = target.offset();
                                var parentOffset = target.offsetParent().offset();
                                distanceFromParentTop = targetOffset.top - parentOffset.top;
                            }

                            // console.log((currentLine)*lineHeight);
                            // console.log(distanceFromParentTop-lineHeight);

                            var topPos=(height-lineHeight)/2-((distanceFromParentTop-lineHeight)||(currentLine)*lineHeight);

                            $list.animate({
                                'top':topPos+'px'
                            },400,function(){

                            });
                            childList.animate({
                                'top':(-20 * currentLine)+'px'
                            },400,function(){

                            });
                            break;
                        }
                    }
                }else{
                    for(var i=currentLine;i>0;i--){
                        if(arrTime[i-1]<pos||i==0){
                            var height=$box.height();
                            currentLine=i;

                            $list.find('li.active').removeClass('active');
                            $list.find('li#line-'+currentLine).addClass('active');

                            var topPos=(height-lineHeight)/2-(currentLine)*lineHeight;

                            $list.animate({
                                'top':topPos+'px'
                            },400,function(){

                            });
                            childList.animate({
                                'top':(-20 * currentLine)+'px'
                            },400,function(){

                            });
                            break;
                        }
                    }                    
                }
            },o.speed);
        });
    };

    $.fn.jqlyric.defaults={
        lyric:'[00:00.00]未找到歌词',   // 歌词字符串 (lrc格式)
        speed:500,                      // 歌词进度一首歌间隔(毫秒)
        getPosition:function(){         // 获取播放器当前播放位置
            gtime+=0.5;
            return gtime;
        }
    }

    function toSeconds(t){     //把形如：01：25的时间转化成秒；
        var m=t.substring(0,t.indexOf(":"));
        var s=t.substring(t.indexOf(":")+1);
        s=parseInt(s.replace(/\b(0+)/gi,""));
        if(isNaN(s))
            s=0;
        var totalt=parseInt(m)*60+s;
        if(isNaN(totalt))
            return 0;
        return totalt;
    }

})(jQuery);