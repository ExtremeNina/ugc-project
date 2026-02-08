// 页面加载完成后执行
window.addEventListener('DOMContentLoaded', function() {
    // 初始化标签切换功能
    initTagSwitching();
    
    // 初始化按钮动画效果
    initButtonAnimations();
    
    // 初始化平滑滚动
    initSmoothScroll();
    
    // 初始化卡片悬停效果
    initCardHoverEffects();
    
    // 初始化响应式菜单
    initResponsiveMenu();
    
    // 初始化加载更多功能
    initLoadMore();
    
    // 添加页面滚动时的导航栏样式变化
    initNavbarScrollEffect();
    
    // 添加随机动画效果给元素
    addRandomAnimations();
});

// 标签切换功能
function initTagSwitching() {
    const tags = document.querySelectorAll('.tag-item');
    
    tags.forEach(tag => {
        tag.addEventListener('click', function() {
            // 移除所有标签的活跃状态
            tags.forEach(t => t.classList.remove('tag-active'));
            // 添加当前标签的活跃状态
            this.classList.add('tag-active');
            
            // 添加点击动画效果
            this.style.transform = 'scale(0.95)';
            setTimeout(() => {
                this.style.transform = 'scale(1)';
            }, 150);
            
            // 这里可以添加根据标签筛选内容的逻辑
            // filterArticlesByTag(this.querySelector('span:first-child').textContent);
        });
    });
}

// 按钮动画效果
function initButtonAnimations() {
    const buttons = document.querySelectorAll('.anime-button');
    
    buttons.forEach(button => {
        button.addEventListener('mousedown', function() {
            this.style.transform = 'scale(0.95) translateY(0)';
        });
        
        button.addEventListener('mouseup', function() {
            this.style.transform = 'scale(1) translateY(-2px)';
        });
        
        button.addEventListener('mouseleave', function() {
            this.style.transform = 'scale(1) translateY(0)';
        });
    });
}

// 平滑滚动功能
function initSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            
            const targetId = this.getAttribute('href');
            if (targetId === '#') return;
            
            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                window.scrollTo({
                    top: targetElement.offsetTop - 80, // 考虑导航栏高度
                    behavior: 'smooth'
                });
            }
        });
    });
}

// 卡片悬停效果增强
function initCardHoverEffects() {
    const cards = document.querySelectorAll('.anime-card');
    
    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            // 添加轻微的旋转效果
            const randomRotation = (Math.random() - 0.5) * 2; // -1 到 1 度
            this.style.transform = `translateY(-5px) rotate(${randomRotation}deg)`;
            this.style.boxShadow = '0 15px 30px rgba(255, 105, 180, 0.2)';
        });
        
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) rotate(0)';
            this.style.boxShadow = '0 4px 15px rgba(255, 105, 180, 0.1)';
        });
    });
}

// 响应式菜单
function initResponsiveMenu() {
    // 在移动设备上显示/隐藏菜单的逻辑
    const menuToggle = document.createElement('button');
    menuToggle.className = 'menu-toggle anime-button';
    menuToggle.innerHTML = '<i class="fa fa-bars"></i>';
    
    const header = document.querySelector('.nav-header');
    const nav = document.querySelector('.main-nav');
    
    if (header && nav) {
        header.appendChild(menuToggle);
        
        menuToggle.addEventListener('click', function() {
            nav.style.display = nav.style.display === 'none' || nav.style.display === '' ? 'block' : 'none';
        });
        
        // 响应式处理
        function handleResize() {
            if (window.innerWidth > 768) {
                nav.style.display = 'block';
            } else {
                nav.style.display = 'none';
            }
        }
        
        window.addEventListener('resize', handleResize);
        handleResize(); // 初始化状态
    }
}

// 加载更多功能
function initLoadMore() {
    const loadMoreBtn = document.querySelector('.load-more-btn');
    
    if (loadMoreBtn) {
        loadMoreBtn.addEventListener('click', function() {
            // 添加加载中状态
            this.innerHTML = '<i class="fa fa-circle-o-notch fa-spin"></i> 加载中...';
            this.disabled = true;
            
            // 模拟加载延迟
            setTimeout(() => {
                // 恢复按钮状态
                this.innerHTML = '<span>加载更多</span><span class="btn-decoration">📜</span>';
                this.disabled = false;
                
                // 这里可以添加实际的加载更多文章的逻辑
                showToast('已加载全部内容~');
            }, 1500);
        });
    }
}

// 导航栏滚动效果
function initNavbarScrollEffect() {
    const header = document.querySelector('.nav-header');
    let lastScrollTop = 0;
    
    window.addEventListener('scroll', function() {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        
        if (scrollTop > 100) {
            header.style.boxShadow = '0 5px 20px rgba(0, 0, 0, 0.1)';
            header.style.padding = '10px 0';
        } else {
            header.style.boxShadow = 'none';
            header.style.padding = '15px 0';
        }
        
        lastScrollTop = scrollTop;
    });
}

// 添加随机动画效果
function addRandomAnimations() {
    const elements = document.querySelectorAll('.article-item-title, .hot-article-content h4, .tutorial-info h4');
    
    elements.forEach(element => {
        element.addEventListener('mouseenter', function() {
            this.style.transition = 'all 0.3s ease';
            this.style.transform = `scale(1.02)`;
            this.style.color = '#ff69b4';
        });
        
        element.addEventListener('mouseleave', function() {
            this.style.transform = `scale(1)`;
            this.style.color = '';
        });
    });
    
    // 给标签添加点击特效
    const allTags = document.querySelectorAll('.tag');
    allTags.forEach(tag => {
        tag.addEventListener('click', function() {
            this.style.animation = 'pulse 0.5s ease';
            setTimeout(() => {
                this.style.animation = '';
            }, 500);
        });
    });
}

// 显示提示消息
function showToast(message) {
    // 创建toast元素
    const toast = document.createElement('div');
    toast.className = 'anime-toast';
    toast.textContent = message;
    
    // 添加样式
    Object.assign(toast.style, {
        position: 'fixed',
        top: '20%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        background: 'linear-gradient(135deg, #ff69b4, #87ceeb)',
        color: 'white',
        padding: '12px 24px',
        borderRadius: '25px',
        boxShadow: '0 5px 15px rgba(255, 105, 180, 0.3)',
        zIndex: '9999',
        fontSize: '14px',
        fontWeight: 'bold',
        opacity: '0',
        transition: 'all 0.3s ease'
    });
    
    // 添加到页面
    document.body.appendChild(toast);
    
    // 显示动画
    setTimeout(() => {
        toast.style.opacity = '1';
        toast.style.transform = 'translate(-50%, 0)';
    }, 100);
    
    // 3秒后隐藏
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translate(-50%, -50%)';
        setTimeout(() => {
            document.body.removeChild(toast);
        }, 300);
    }, 3000);
}

// 模拟搜索功能
function initSearchFunction() {
    const searchInput = document.querySelector('.search-input');
    const searchBtn = document.querySelector('.search-btn');
    
    if (searchInput && searchBtn) {
        searchBtn.addEventListener('click', function() {
            performSearch();
        });
        
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                performSearch();
            }
        });
        
        function performSearch() {
            const query = searchInput.value.trim();
            if (query) {
                showToast(`搜索: ${query}`);
                // 这里可以添加实际的搜索逻辑
            } else {
                showToast('请输入搜索内容~');
            }
        }
    }
}

// 初始化搜索功能
initSearchFunction();

// 添加滚动到顶部按钮
function initScrollToTopButton() {
    const scrollBtn = document.createElement('button');
    scrollBtn.className = 'scroll-to-top anime-button';
    scrollBtn.innerHTML = '<i class="fa fa-arrow-up"></i>';
    
    Object.assign(scrollBtn.style, {
        position: 'fixed',
        bottom: '30px',
        right: '30px',
        width: '50px',
        height: '50px',
        borderRadius: '50%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '0',
        fontSize: '20px',
        opacity: '0',
        visibility: 'hidden',
        transition: 'all 0.3s ease',
        zIndex: '999'
    });
    
    document.body.appendChild(scrollBtn);
    
    scrollBtn.addEventListener('click', function() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
    
    window.addEventListener('scroll', function() {
        if (window.pageYOffset > 500) {
            scrollBtn.style.opacity = '1';
            scrollBtn.style.visibility = 'visible';
        } else {
            scrollBtn.style.opacity = '0';
            scrollBtn.style.visibility = 'hidden';
        }
    });
}

// 初始化回到顶部按钮
initScrollToTopButton();

// 添加点赞功能
function initLikeButtons() {
    const likeButtons = document.querySelectorAll('.like-count');
    
    likeButtons.forEach(button => {
        button.addEventListener('click', function() {
            const countElement = this.querySelector('span');
            let count = parseInt(countElement.textContent.replace(/,/g, ''));
            
            if (this.classList.contains('liked')) {
                count--;
                this.classList.remove('liked');
                this.querySelector('i').style.color = '#ff69b4';
            } else {
                count++;
                this.classList.add('liked');
                this.querySelector('i').style.color = '#ff4757';
                
                // 添加点赞动画
                this.querySelector('i').style.animation = 'pulse 0.5s ease';
                setTimeout(() => {
                    this.querySelector('i').style.animation = '';
                }, 500);
            }
            
            // 格式化数字显示
            countElement.textContent = count.toLocaleString();
        });
    });
}

// 初始化点赞功能
initLikeButtons();

// 添加文章阅读时间估算
function addReadTimeEstimates() {
    const articles = document.querySelectorAll('.article-item');
    
    articles.forEach(article => {
        const excerpt = article.querySelector('.article-item-excerpt');
        if (excerpt) {
            // 简单估算：假设平均阅读速度为每分钟200字
            const wordCount = excerpt.textContent.length;
            const readTime = Math.max(1, Math.ceil(wordCount / 200));
            
            // 创建阅读时间元素
            const readTimeElement = document.createElement('span');
            readTimeElement.className = 'read-time';
            readTimeElement.innerHTML = `<i class="fa fa-clock-o"></i> ${readTime}分钟阅读`;
            readTimeElement.style.marginLeft = '10px';
            readTimeElement.style.color = '#87ceeb';
            
            // 添加到统计信息区域
            const statsContainer = article.querySelector('.article-item-stats');
            if (statsContainer) {
                statsContainer.appendChild(readTimeElement);
            }
        }
    });
}

// 添加阅读时间估算
addReadTimeEstimates();