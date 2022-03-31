package com.xioshe.only.java.base.misc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;

/**
 * 演示 Unsafe 的各种方法的功能
 *
 * @author xioshe 2022-03-31
 */
@ExtendWith(MockitoExtension.class)
class SomethingUnsafeTest {

    static Unsafe unsafe;

    @BeforeAll
    static void setUp() throws NoSuchFieldException, IllegalAccessException {
        unsafe = SomethingUnsafe.stealInstance();
    }

    // 内存操作

    @Test
    void set_memory_demo() {
        // 在堆外内存上开辟一个 4 字节的空间，返回内存地址
        // 右边是低地址位，左边是高地址位，可以保持和 BE 的一致
        // [?][?][?][?] <- addr
        long addr = unsafe.allocateMemory(4);
        try {
            assertThat(addr).isGreaterThan(0);
            System.out.println("4 字节内存地址为" + addr);
            // 从地位字节开始，按字节填充值，填充字节段长度为 2
            // [?][?][1][1] <- addr
            unsafe.setMemory(addr, 2, (byte) 1);
            int value1 = unsafe.getInt(addr);
            assertThat(value1).as("两个低位字节都为1").isEqualTo(257)
                    .extracting(Integer::toBinaryString)
                    .isEqualTo("100000001");
            // [255][127][1][1] <- addr
            unsafe.setMemory(addr + 2, 1, (byte) 127);
            unsafe.setMemory(addr + 3, 1, (byte) 255);
            int v2 = unsafe.getInt(addr);
            System.out.println(v2);
            assertThat(v2).as("最高位为1，是负数").isNegative()
                    .extracting(Integer::toBinaryString)
                    .isEqualTo("11111111011111110000000100000001");
        } finally {
            // 回收 off heap
            unsafe.freeMemory(addr);
        }

    }

    @Test
    void reallocate_memory_demo() {
        int size = 4;
        // [1][1][1][1] <- addr
        long addr = unsafe.allocateMemory(size);
        try {
            unsafe.setMemory(addr, size, (byte) 1);
            assertThat(unsafe.getInt(addr))
                    .extracting(Integer::toBinaryString)
                    .isEqualTo("1000000010000000100000001");
            // addr 处重新分配 8 字节的堆外内存，不改变之前的 4 字节
            // [?][?][?][?][1][1][1][1] <- addr
            long newAddr = unsafe.reallocateMemory(addr, size << 1);
            assertThat(addr).as("重新分配后地址不会变").isEqualTo(newAddr);
            assertThat(unsafe.getInt(addr)).as("重新分配后原值仍然存在")
                    .extracting(Integer::toBinaryString)
                    .isEqualTo("1000000010000000100000001");
            assertThat(unsafe.getLong(addr))
                    .extracting(Long::toBinaryString)
                    .isEqualTo("1000000010000000100000001");
        } finally {
            unsafe.freeMemory(addr);
        }
    }

    @Test
    void copy_memory_demo() {
        long addr = unsafe.allocateMemory(4);
        // [][][][] <- addr
        try {
            // [][][2][2] <- addr
            unsafe.setMemory(addr, 2, (byte) 2);
            assertThat(unsafe.getInt(addr))
                    .extracting(Integer::toBinaryString)
                    .isEqualTo("1000000010");
            // 将 addr 处开始的 1 字节内容复制到 addr + 2 处
            // [][2][2][2] <- addr
            unsafe.copyMemory(addr, addr + 2, 1);
            assertThat(unsafe.getInt(addr))
                    .extracting(Integer::toBinaryString)
                    .isEqualTo("100000001000000010");
        } finally {
            unsafe.freeMemory(addr);
        }
    }

    // CAS 操作

    @Test
    void cas_sync_demo() {
        class IntegerIncreaser {
            private volatile int val;

            private static final Long OFFSET;

            static {
                long l;
                try {
                    l = unsafe.objectFieldOffset(IntegerIncreaser.class.getDeclaredField("val"));
                } catch (NoSuchFieldException e) {
                    l = 0L;
                }
                OFFSET = l;
            }

            private void increaseWhenEquals(int newVal) {
                while (!unsafe.compareAndSwapInt(this, OFFSET, newVal, newVal + 1)) ;
            }
        }

        IntegerIncreaser increaser = new IntegerIncreaser();

        // 两个子线程按顺序修改 val
        CountDownLatch allDone = new CountDownLatch(2);
        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                increaser.increaseWhenEquals(i);
                System.out.print(increaser.val + " ");
            }
            allDone.countDown();
        }).start();
        new Thread(() -> {
            for (int i = 5; i < 10; i++) {
                increaser.increaseWhenEquals(i);
                System.out.print(increaser.val + " ");
            }
            allDone.countDown();
        }).start();

        try {
            allDone.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("主线程结束");
    }

    // 内存屏障

    @Test
    void memory_fence_demo() {
        class FlagChanger implements Runnable {
            // 不需要 volatile 也能保证可见性
            boolean changed = false;

            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("1. 子线程改变内部状态");
                changed = true;
            }
        }
        var flagChanger = new FlagChanger();
        new Thread(flagChanger).start();

        while (true) {
            boolean changed = flagChanger.changed;
            // 读屏障，保证 changed 读取到的是最新值
            // 如果没有此行，主线程会死循环
            unsafe.loadFence();
            if (changed) {
                System.out.println("2. 主线成监测到子线程状态改变");
                break;
            }
        }
        System.out.println("3. 主线程结束");
    }

    // 对象操作

    @Test
    void access_field_of_object() throws NoSuchFieldException {
        User user = new User("xioshe", 97);
        Field nameField = User.class.getDeclaredField("name");
        long nameOffset = unsafe.objectFieldOffset(nameField);
        String nobody = "nobody";
        String name = (String) unsafe.getAndSetObject(user, nameOffset, nobody);
        assertThat(name).as("可以通过内存操作获取对象的属性值").isEqualTo("xioshe");
        assertThat(user.name).as("内存修改属性值，无视 final 修饰符").isEqualTo(nobody);

        Field ageField = User.class.getDeclaredField("age");
        long ageOffset = unsafe.objectFieldOffset(ageField);
        char ageToChar = unsafe.getChar(user, ageOffset);
        assertThat(ageToChar).as("int 类型的年龄转换为 char").isEqualTo('a');
    }

    @Test
    void allocate_instance_demo() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        User nobody = new User();
        assertThat(nobody).extracting("name", "age")
                .contains("nobody", atIndex(0))
                .contains(-1, atIndex(1));
        User somebody = User.class.getDeclaredConstructor().newInstance();
        assertThat(somebody).extracting("name", "age")
                .contains("nobody", atIndex(0))
                .contains(-1, atIndex(1));
        User frankenstein = (User) unsafe.allocateInstance(User.class);
        assertThat(frankenstein).as("通过 allocateInstance 创建的对象不会执行实例初始化代码")
                .extracting("name", "age")
                .contains(null, atIndex(0))
                .contains(0, atIndex(1));
    }

    // 数组操作

    @Test
    void visit_array_demo() {
        int base = unsafe.arrayBaseOffset(String[].class);
        assertThat(base).isEqualTo(16);
        int scale = unsafe.arrayIndexScale(String[].class);
        assertThat(scale).isEqualTo(4);
        String[] sx = {"zero", "one", "two"};
        for (int i = 0; i < sx.length; i++) {
            int offset = base + scale * i;
            assertThat(unsafe.getObject(sx, offset)).isEqualTo(sx[i]);
        }
    }


    // 线程调度
    @Test
    void park_thread_demo() {
        Thread testThread = Thread.currentThread();
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("2. 子线程 unpark 主线程");
            unsafe.unpark(testThread);
        }).start();

        System.out.println("1. 主线程 parked");
        // 阻塞当前线程，isAbsolute 跟 time 是纳秒还是毫秒有关
        unsafe.park(false, 0L);
        System.out.println("3. 主线程 unpark");
    }


    // Class 操作

    @Test
    void access_static_field() throws IllegalAccessException, NoSuchFieldException {
        MethodHandles.lookup().ensureInitialized(User.class);
        Field field = User.class.getDeclaredField("MARK");
        Object base = unsafe.staticFieldBase(field);
        long offset = unsafe.staticFieldOffset(field);
        assertThat(unsafe.getChar(base, offset)).isEqualTo('a');
    }

    @Test
    @Disabled
    void define_class_demo() {

    }

    // 系统信息

    @Test
    void get_system_info_demo() {
        int pageSize = unsafe.pageSize();
        assertThat(pageSize).isEqualTo(4096);
        int addressSize = unsafe.addressSize();
        assertThat(addressSize).isEqualTo(8);
    }


    private static final class User {
        private static final char MARK = 'a';
        private final String name;
        private final int age;

        public User() {
            name = "nobody";
            age = -1;
        }

        private User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
